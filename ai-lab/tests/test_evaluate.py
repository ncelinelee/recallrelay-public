import unittest
import sys
import json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from evaluate import evaluate_response, evaluate_file, response_hash

class EvaluationTests(unittest.TestCase):
    case={"interaction":"Customer confirmed Sep 3 appointment."}
    raw=json.dumps({"exists":True,"commitment_type":"Appointment","date_or_condition":"Sep 3","evidence":"Customer confirmed Sep 3 appointment."})
    def test_supported_response(self):
        result=evaluate_response(self.raw,self.case)
        self.assertTrue(result["schema_valid"]);self.assertTrue(result["evidence_supported"]);self.assertTrue(result["date_supported"])
    def test_invalid_json(self):self.assertFalse(evaluate_response("not JSON",self.case)["json_valid"])
    def test_legacy_type_field_is_not_current_schema(self):
        self.assertFalse(evaluate_response(self.raw.replace('commitment_type','type'),self.case)["schema_valid"])
    def test_fabricated_evidence_rejected(self):
        self.assertFalse(evaluate_response(self.raw.replace('Customer confirmed Sep 3 appointment.','Promise to complete tomorrow'),self.case)["evidence_supported"])
    def test_stale_manual_judgments_rejected(self):
        with self.assertRaises(ValueError):evaluate_response(self.raw,self.case,{"response_sha256":response_hash("old output"),"judgments":{}})
    def test_bound_judgments_loaded_without_claiming_automatic_accuracy(self):
        r=evaluate_response(self.raw,self.case,{"response_sha256":response_hash(self.raw),"judgments":{"correct":True}})
        self.assertEqual(r["human_review"],{"correct":True})
    def test_unsupported_date_rejected(self):
        self.assertFalse(evaluate_response(self.raw.replace('"Sep 3"','"Sep 9"'),self.case)["date_supported"])
    def test_duplicate_and_unknown_cases_rejected(self):
        row={"case_id":"A","prompt_version":"v3","raw_response":self.raw}
        with self.assertRaises(ValueError):evaluate_file([row,row],{"A":self.case})
        with self.assertRaises(ValueError):evaluate_file([row],{})
if __name__ == "__main__":unittest.main()
