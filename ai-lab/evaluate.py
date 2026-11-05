"""Offline re-evaluation of stored promise responses; no API calls or manual-score reuse."""
import argparse
import csv
import hashlib
import json
from pathlib import Path

REQUIRED = {"exists", "commitment_type", "date_or_condition", "evidence"}
def response_hash(raw):
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()

def evaluate_response(raw, case, human=None):
    result = {"response_sha256": response_hash(raw), "json_valid": False,
        "schema_valid": False, "evidence_supported": False, "date_supported": False,
        "human_review": None}
    if human is not None:
        if human.get("response_sha256") != result["response_sha256"]:
            raise ValueError("Stale human review: response hash does not match")
        result["human_review"] = human.get("judgments")
    try: data = json.loads(raw)
    except (ValueError, TypeError): return result
    result["json_valid"] = True
    if not isinstance(data,dict) or set(data) != REQUIRED or type(data.get("exists")) is not bool:
        return result
    if any(data[k] is not None and not isinstance(data[k],str) for k in REQUIRED - {"exists"}): return result
    if data["exists"]:
        if any(not isinstance(data[k],str) or not data[k].strip() for k in REQUIRED - {"exists"}): return result
    elif any(data[k] is not None for k in REQUIRED - {"exists"}): return result
    result["schema_valid"] = True
    if not data["exists"]:
        result["evidence_supported"] = True
        result["date_supported"] = True
    else:
        result["evidence_supported"] = data["evidence"] in case["interaction"]
        # Exact, case-insensitive phrase support is a conservative mechanical check,
        # not a semantic date parser and not commitment-extraction accuracy.
        result["date_supported"] = data["date_or_condition"].casefold() in case["interaction"].casefold()
    return result

def evaluate_file(responses, cases, human_reviews=None):
    reviews = human_reviews or {}
    rows = []
    seen = set()
    for row in responses:
        key = row["case_id"] + "/" + row["prompt_version"]
        if key in seen: raise ValueError("Duplicate response identifier: " + key)
        seen.add(key)
        if row["case_id"] not in cases: raise ValueError("Unknown case: " + row["case_id"])
        checked = evaluate_response(row["raw_response"], cases[row["case_id"]], reviews.get(key))
        rows.append({"case_id":row["case_id"], "prompt_version":row["prompt_version"], **checked})
    unknown = set(reviews)-seen
    if unknown: raise ValueError("Human reviews without matching responses: " + str(sorted(unknown)))
    versions = {}
    for version in sorted({x["prompt_version"] for x in rows}):
        group = [x for x in rows if x["prompt_version"]==version]
        versions[version] = {"n":len(group), **{k:sum(x[k] for x in group)/len(group) for k in
            ["json_valid","schema_valid","evidence_supported","date_supported"]}}
    return {"scope":"stored development responses; not held-out accuracy", "versions":versions, "rows":rows}

if __name__ == "__main__":
    root=Path(__file__).resolve().parent
    p=argparse.ArgumentParser();p.add_argument("--responses",type=Path,default=root/"results/prompt_experiment.csv")
    p.add_argument("--cases",type=Path,default=root/"tests/ground_truth_cases.json")
    p.add_argument("--human",type=Path);p.add_argument("--output",type=Path)
    args=p.parse_args()
    with args.responses.open(encoding="utf-8") as f: responses=list(csv.DictReader(f))
    report=evaluate_file(responses,json.loads(args.cases.read_text()),json.loads(args.human.read_text()) if args.human else None)
    output=json.dumps(report,ensure_ascii=False,indent=2)+"\n"
    if args.output:args.output.write_text(output,encoding="utf-8")
    else:print(output)
