<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { recallRelayApi } from '../services/api'
import { session } from '../stores/session'
import { locale, tr } from '../i18n'

const router = useRouter()
const route = useRoute()

function emptyCaseDetail() {
  return {
    id: '',
    organization: 'N/A',
    dealership: 'N/A',
    caseStatus: 'N/A',
    operationalPriority: 'N/A',
    nextAction: 'Loading case data',
    nextActionReason: 'Waiting for Backend API response.',
    customerNeed: {
      requestedDate: 'N/A',
      source: 'Customer requested service date',
    },
    customer: {
      no: 'N/A',
      name: 'N/A',
      phone: 'N/A',
    },
    vehicle: {
      model: 'N/A',
      vin: 'N/A',
      plate: 'N/A',
    },
    recall: {
      no: 'N/A',
      severity: 'N/A',
      status: 'N/A',
      safetyImpact: 'N/A',
    },
    part: {
      no: 'N/A',
      status: 'N/A',
      name: 'N/A',
      eta: 'N/A',
      orderNo: 'N/A',
    },
    appointment: {
      date: 'N/A',
      time: 'N/A',
      technician: 'N/A',
      status: 'N/A',
      readiness: 'NOT_READY',
    },
    service: {
      technician: 'N/A',
      status: 'NOT_STARTED',
      updatedAt: 'N/A',
    },
    eligibility: {
      ownership: false,
      serviceArea: false,
      phoneValidity: false,
    },
    activities: [],
  }
}

const caseDetail = reactive(emptyCaseDetail())

const editingEligibility = ref(false)
const ownership = ref(false)
const serviceArea = ref(false)
const phoneValidity = ref(false)
const activities = ref([])
const activityModalOpen = ref(false)
const activityMode = ref('CONTACT')
const activitySummary = ref('')
const contactChannel = ref('Phone')
const contactOutcome = ref('Reached')
const callbackDate = ref('')
const serviceStatus = ref('NOT_STARTED')
const editingService = ref(false)
const feedback = ref('')
const aiError = ref('')
const intelligenceLoading = ref(false)

const promiseIntegrityResult = ref(null)
const nextBestQuestionResult = ref(null)
const contradictionResult = ref(null)

const overpromiseDraft = ref('')
const overpromiseResult = ref(null)
const overpromiseLoading = ref(false)

const proposedEta = ref('')
const changeImpactResult = ref(null)
const changeImpactLoading = ref(false)

function aiT(en, ko) {
  return locale.value === 'ko' ? ko : en
}

function aiStatusLabel(value) {
  if (!value) return ''

  if (locale.value !== 'ko') {
    return String(value).replaceAll('_', ' ')
  }

  const labels = {
    NO_COMMITMENT: '약속 없음',
    WATCH: '주의',
    AT_RISK: '위험',
    ALIGNED: '충족',
    HIGH: '높음',
    LOW: '낮음',
    NONE: '없음',

    IN_TRANSIT: '운송 중',
    ARRIVED: '도착 완료',
    REQUESTED: '요청됨',
    ORDERED: '주문 완료',
    READY: '준비 완료',
    NOT_READY: '준비 안 됨',
  }

  return labels[value] || String(value).replaceAll('_', ' ')
}

function aiStateClass(value) {
  if (['HIGH', 'AT_RISK'].includes(value)) {
    return 'ai-state--danger'
  }

  if (['NONE', 'ALIGNED', 'NO_COMMITMENT'].includes(value)) {
    return 'ai-state--ok'
  }

  return ''
}

function aiPromisePrimary(result) {
  if (!result) return ''

  if (!result.commitmentExists) {
    return aiT(
      'No explicit customer commitment',
      '명시적인 고객 약속이 없습니다.'
    )
  }

  return locale.value === 'ko'
    ? `${result.commitmentType || '고객 약속'}이 감지되었습니다.`
    : result.commitmentType || 'Customer commitment detected'
}

function aiPromiseEvidence(result) {
  if (!result) return ''

  if (!result.evidence) {
    return aiT(
      'No explicit customer-facing commitment found in the recorded interaction.',
      '기록된 고객 응대 내용에서 명시적인 고객 약속을 찾지 못했습니다.'
    )
  }

  /*
   * 실제 고객 발화나 기록 자체는 Evidence이므로
   * 번역해서 바꾸지 않고 원문을 그대로 보여준다.
   */
  return result.evidence
}

function aiTimelineSummary(result) {
  if (!result) return ''

  const count = result.activitiesReviewed || 0

  return locale.value === 'ko'
    ? `${count}개의 타임라인 기록을 시간 정보와 함께 검토했습니다.`
    : `${count} timeline record(s) reviewed with timestamp context.`
}

function aiConflictReview(result) {
  if (!result) return ''

  if (!result.hasPossibleConflict) {
    return aiT(
      'No meaningful cross-source contradiction detected.',
      '서로 다른 데이터 출처 사이에서 의미상 충돌을 발견하지 못했습니다.'
    )
  }

  const count = result.conflicts?.length || 0

  return locale.value === 'ko'
    ? `${count}건의 의미상 충돌 후보가 발견되어 직원 확인이 필요합니다.`
    : `${count} possible cross-source conflict(s) require human review.`
}

function aiMissingFieldLabel(field) {
  if (locale.value !== 'ko') return field

  const labels = {
    CUSTOMER_REQUESTED_DATE: '고객 요청 서비스 날짜',
    LATEST_PART_ETA: '최신 부품 ETA',
  }

  return labels[field] || field
}

function aiVerifyPrimary(result) {
  if (!result) return ''

  if (!result.informationMissing) {
    return aiT(
      'All required information is available.',
      '필요한 정보가 모두 확인되어 있습니다.'
    )
  }

  if (result.missingFields?.includes('CUSTOMER_REQUESTED_DATE')) {
    return aiT(
      'What date did the customer request for recall service?',
      '고객이 리콜 서비스를 원하는 날짜를 확인하세요.'
    )
  }

  if (result.missingFields?.includes('LATEST_PART_ETA')) {
    return aiT(
      'What is the latest expected arrival date for the recall part?',
      '리콜 부품의 최신 예상 도착일을 확인하세요.'
    )
  }

  return aiT(
    'Verify the missing case information first.',
    '누락된 Case 정보를 먼저 확인하세요.'
  )
}

function aiVerifyReason(result) {
  if (!result) return ''

  if (!result.informationMissing) {
    return aiT(
      'No verification question was generated. The LLM call was skipped.',
      '추가 확인 질문이 필요하지 않아 LLM 호출을 생략했습니다.'
    )
  }

  if (result.missingFields?.includes('CUSTOMER_REQUESTED_DATE')) {
    return aiT(
      'The customer requested service date is missing and should be verified before further supply decisions.',
      '고객 요청 서비스 날짜가 누락되어 있어 공급 관련 판단 전에 먼저 확인해야 합니다.'
    )
  }

  if (result.missingFields?.includes('LATEST_PART_ETA')) {
    return aiT(
      'The latest part ETA is missing and should be verified before a firm customer commitment.',
      '최신 부품 ETA가 없어 고객에게 확정적인 안내를 하기 전에 먼저 확인해야 합니다.'
    )
  }

  return result.reason || ''
}

function aiChangeSummary(result) {
  if (!result) return ''

  if (locale.value !== 'ko') {
    return result.changeSummary
  }

  const beforeEta =
    result.changes?.eta?.before ||
    caseDetail.part.eta

  const afterEta =
    result.changes?.eta?.after ||
    proposedEta.value

  const needDate = caseDetail.customerNeed.requestedDate

  let text =
    `부품 ETA가 ${beforeEta}에서 ${afterEta}로 변경됩니다. ` +
    `공급 충족 상태는 ${aiStatusLabel(result.supplyAlignmentBefore)}에서 ` +
    `${aiStatusLabel(result.supplyAlignmentAfter)}으로 바뀝니다.`

  if (needDate && needDate !== 'N/A') {
    text += ` 현재 고객 요청일은 ${needDate}입니다.`
  }

  return text
}

function aiWorkflowImpact(result) {
  if (!result) return ''

  if (locale.value !== 'ko') {
    return (
      result.workflowImpact ||
      'No material workflow impact identified.'
    )
  }

  if (result.affectedAppointment) {
    return (
      `현재 예약 ${caseDetail.appointment.date} ${caseDetail.appointment.time}에 ` +
      `영향을 줄 수 있어 변경된 ETA와 예약 일정을 다시 확인해야 합니다.` +
      (result.promiseRiskAfter === 'NO_COMMITMENT'
        ? ' 기존 고객 약속은 확인되지 않았습니다.'
        : '')
    )
  }

  return '현재 예약이나 주요 업무 흐름에 의미 있는 영향이 확인되지 않았습니다.'
}

function aiOverpromiseReason(result) {
  if (!result) return ''

  if (locale.value !== 'ko') {
    return result.reason || 'No unsupported wording detected.'
  }

  if (result.risk === 'HIGH') {
    const phrase = result.unsupportedPhrase
      ? `"${result.unsupportedPhrase}"`
      : '현재 표현'

    return (
      `현재 ETA는 예상 도착일일 뿐 실제 부품 도착이 확인된 것은 아닙니다. ` +
      `따라서 ${phrase}처럼 확정적으로 표현하면 현재 공급 사실보다 강한 약속이 됩니다.`
    )
  }

  if (result.risk === 'NONE') {
    return (
      '현재 문구는 ETA를 확정 도착으로 표현하지 않아 ' +
      '현재 확인된 공급 사실과 일치합니다.'
    )
  }

  return '현재 공급 사실과 고객 안내 문구의 확정성을 다시 확인해야 합니다.'
}



const canManageAppointment = computed(() =>
  ['APPOINTMENT_COORDINATOR', 'SERVICE_ADVISOR', 'SERVICE_MANAGER'].includes(session.role)
)

const canLogCustomerContact = computed(() =>
  ['APPOINTMENT_COORDINATOR', 'SERVICE_ADVISOR', 'SERVICE_MANAGER'].includes(session.role)
)

const canEditPart = computed(() =>
  ['PARTS_SPECIALIST', 'SERVICE_MANAGER'].includes(session.role)
)

const canEditService = computed(() =>
  ['TECHNICIAN', 'SERVICE_MANAGER'].includes(session.role)
)

function formatDate(value) {
  if (!value) return 'N/A'
  return String(value).slice(0, 10)
}

function formatTime(value) {
  if (!value || !String(value).includes('T')) return 'N/A'
  return String(value).split('T')[1].slice(0, 5)
}

function formatDateTime(value) {
  if (!value) return 'N/A'
  return String(value).replace('T', ' ').slice(0, 16)
}

function deriveNextAction(data, part) {
  const requestedDate = data.requestedServiceDate
  const eta = part?.eta
  const partStatus = part?.status

  if (!requestedDate) {
    return {
      action: 'Verify customer requested service date',
      reason: 'Customer timing is required before supply alignment can be evaluated.',
    }
  }

  if (partStatus === 'ARRIVED') {
    return {
      action: 'Proceed with scheduled recall service',
      reason: 'Required recall part has physically arrived.',
    }
  }

  if (!eta) {
    return {
      action: 'Verify latest part ETA',
      reason: 'Part ETA is missing while customer timing is known.',
    }
  }

  if (eta > requestedDate) {
    return {
      action: 'Align customer timing with part supply',
      reason: 'Current part ETA is later than the customer requested service date.',
    }
  }

  return {
    action: 'Confirm physical part arrival before appointment',
    reason: 'ETA is before the requested service date, but the part has not physically arrived.',
  }
}

function normalizeApiCase(data) {
  const part = data.parts?.[0] || {}
  const appointment = data.appointment || {}
  const next = deriveNextAction(data, part)

  return {
    id: data.caseId,
    organization: data.organization?.name || 'N/A',
    dealership: data.dealership?.name || 'N/A',
    caseStatus: data.caseStatus || 'N/A',
    operationalPriority: data.operationalPriority || 'N/A',
    nextAction: next.action,
    nextActionReason: next.reason,

    customerNeed: {
      requestedDate: data.requestedServiceDate || 'N/A',
      source: 'Customer requested service date',
    },

    customer: {
      no: data.customer?.customerNo || 'N/A',
      name: data.customer?.name || 'N/A',
      phone: data.customer?.phone || 'N/A',
    },

    vehicle: {
      model: 'N/A',
      vin: data.vehicle?.vin || 'N/A',
      plate: 'N/A',
    },

    recall: {
      no: data.recall?.recallNo || 'N/A',
      severity: data.recall?.severity || 'N/A',
      status: data.recall?.campaignStatus || 'N/A',
      safetyImpact: data.recall?.safetyImpact || 'N/A',
    },

    part: {
      no: part.partNo || 'N/A',
      status: part.status || 'N/A',
      name: part.partName || 'N/A',
      eta: part.eta || 'N/A',
      orderNo: part.orderNo || 'N/A',
    },

    appointment: {
      date: formatDate(appointment.scheduledAt),
      time: formatTime(appointment.scheduledAt),
      technician: appointment.technicianName || 'Unassigned',
      status: appointment.appointmentStatus || 'N/A',
      readiness: part.status === 'ARRIVED' ? 'READY' : 'NOT_READY',
    },

    service: {
      technician: appointment.technicianName || 'Unassigned',
      status: appointment.serviceStatus || 'NOT_STARTED',
      updatedAt: formatDateTime(appointment.updatedAt),
    },

    eligibility: {
      ownership: Boolean(data.ownershipValid),
      serviceArea: Boolean(data.serviceAreaValid),
      phoneValidity: Boolean(data.phoneValid),
    },

    activities: (data.activities || []).map((item) => ({
      date: formatDateTime(item.createdAt),
      type: item.activityType || 'MEMO',
      owner: item.employeeName || 'Unknown Employee',
      summary: item.summary || '',
    })),
  }
}

async function loadCase() {
  feedback.value = ''

  try {
    const caseId = Number(route.params.id || 20881)
    const { data } = await recallRelayApi.getCase(caseId)
    const normalized = normalizeApiCase(data)

    Object.assign(caseDetail, normalized)

    ownership.value = normalized.eligibility.ownership
    serviceArea.value = normalized.eligibility.serviceArea
    phoneValidity.value = normalized.eligibility.phoneValidity
    serviceStatus.value = normalized.service.status
    activities.value = normalized.activities.map((item) => ({ ...item }))
  } catch (error) {
    console.error('Failed to load recall case:', error)
    feedback.value =
      error?.response?.data?.message ||
      'Unable to load Recall Case from Backend API.'
  }
}

function isoDiffDays(later, earlier) {
  if (!later || !earlier || later === 'N/A' || earlier === 'N/A') return null

  const ms =
    new Date(`${later}T00:00:00`).getTime() -
    new Date(`${earlier}T00:00:00`).getTime()

  if (Number.isNaN(ms)) return null

  return Math.round(ms / 86400000)
}

const alignment = computed(() => {
  const needDate = caseDetail.customerNeed.requestedDate
  const eta = caseDetail.part.eta

  if (!needDate || needDate === 'N/A') {
    return {
      status: 'AT_RISK',
      reason: tr(
        'Customer requested service date is unavailable. Customer timing must be verified before supply alignment can be evaluated.'
      ),
    }
  }

  if (caseDetail.part.status === 'ARRIVED') {
    return {
      status: 'ALIGNED',
      reason: tr(
        'Required recall part has physically arrived. Supply is available for the current service plan.'
      ),
    }
  }

  if (!eta || eta === 'N/A') {
    return {
      status: 'AT_RISK',
      reason: tr(
        'Customer timing is known, but confirmed part ETA is unavailable. Supply timing must be verified before a firm commitment.'
      ),
    }
  }

  const gap = isoDiffDays(eta, needDate)

  if (gap > 0) {
    return {
      status: 'AT_RISK',
      reason:
        locale.value === 'ko'
          ? `부품 ETA가 고객 요청일보다 ${gap}일 늦습니다. 고객 일정과 공급 계획을 조정해야 합니다.`
          : `Part ETA is ${gap} day${gap === 1 ? '' : 's'} later than the customer requested date. Customer timing and supply plan need alignment.`,
    }
  }

  return {
    status: 'WATCH',
    reason: tr(
      'Current ETA is before the requested service date, but the part is not physically received yet. Verify supply before confirming the service commitment.'
    ),
  }
})

function openActivity(mode) {
  activityMode.value = mode
  activitySummary.value = ''
  callbackDate.value = ''
  feedback.value = ''
  activityModalOpen.value = true
}

function saveActivity() {
  if (!activitySummary.value.trim()) {
    feedback.value = tr('Activity summary is required.')
    return
  }

  const now = new Date()
  const dateLabel = now.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

  const contactPrefix =
    activityMode.value === 'CONTACT'
      ? `${contactChannel.value} · ${contactOutcome.value}. `
      : ''

  const callbackSuffix =
    callbackDate.value ? ` Callback: ${callbackDate.value}.` : ''

  activities.value.unshift({
    date: dateLabel,
    type: activityMode.value,
    owner: session.employeeName || 'Current User',
    summary: `${contactPrefix}${activitySummary.value.trim()}${callbackSuffix}`,
  })

  activityModalOpen.value = false
  feedback.value = tr(
    activityMode.value === 'CONTACT'
      ? 'Customer interaction logged.'
      : 'Case memo added.'
  )
}

function saveServiceStatus() {
  editingService.value = false
  feedback.value =
    locale.value === 'ko'
      ? `정비 상태가 ${tr(serviceStatus.value)}(으)로 변경되었습니다.`
      : `Service status updated to ${serviceStatus.value}.`
}


function aiCaseId() {
  return Number(route.params.id || caseDetail.id || 20881)
}

function aiErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.detail ||
    error?.message ||
    'AI analysis could not be completed.'
  )
}

async function runCaseIntelligence() {
  aiError.value = ''
  intelligenceLoading.value = true

  try {
    const caseId = aiCaseId()

    const [promise, question, contradiction] = await Promise.all([
      recallRelayApi.promiseIntegrity(caseId),
      recallRelayApi.nextBestQuestion(caseId),
      recallRelayApi.checkContradictions(caseId),
    ])

    promiseIntegrityResult.value = promise.data
    nextBestQuestionResult.value = question.data
    contradictionResult.value = contradiction.data
  } catch (error) {
    console.error('Case intelligence failed:', error)
    aiError.value = aiErrorMessage(error)
  } finally {
    intelligenceLoading.value = false
  }
}

async function checkOverpromiseDraft() {
  aiError.value = ''

  if (!overpromiseDraft.value.trim()) {
    aiError.value = 'Enter a customer-facing draft first.'
    return
  }

  overpromiseLoading.value = true

  try {
    const { data } = await recallRelayApi.overpromiseCheck(
      aiCaseId(),
      { draft: overpromiseDraft.value.trim() }
    )

    overpromiseResult.value = data
  } catch (error) {
    console.error('Overpromise check failed:', error)
    aiError.value = aiErrorMessage(error)
  } finally {
    overpromiseLoading.value = false
  }
}

async function analyzeEtaChange() {
  aiError.value = ''

  if (!proposedEta.value) {
    aiError.value = 'Enter a proposed ETA first.'
    return
  }

  if (proposedEta.value === caseDetail.part.eta) {
    aiError.value = 'Proposed ETA must be different from the current ETA.'
    return
  }

  changeImpactLoading.value = true

  try {
    const { data } = await recallRelayApi.changeImpact(
      aiCaseId(),
      {
        afterEta: proposedEta.value,
        afterPartStatus: caseDetail.part.status,
      }
    )

    changeImpactResult.value = data
  } catch (error) {
    console.error('Change Impact analysis failed:', error)
    aiError.value = aiErrorMessage(error)
  } finally {
    changeImpactLoading.value = false
  }
}

onMounted(loadCase)
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="Integrated recall case" :title="`Recall Case RC-${caseDetail.id}`" description="Customer need, supply status, recall workflow and service execution in one case.">
      <button class="button button--secondary" @click="router.push('/search')">{{ $tr('Back to Search') }}</button>
    </PageHeader>

    <div v-if="feedback" class="employee-feedback employee-feedback--success case-feedback">{{ feedback }}</div>

    <div class="case-header">
      <div class="case-header__identity">
        <div><span>{{ $tr('Organization') }}</span><strong>{{ caseDetail.organization }}</strong></div>
        <div><span>{{ $tr('Dealership') }}</span><strong>{{ caseDetail.dealership }}</strong></div>
      </div>
      <div class="case-header__status">
        <div><span>{{ $tr('Case Status') }} <small>{{ $tr('system managed') }}</small></span><StatusBadge :value="caseDetail.caseStatus" /></div>
        <div><span>{{ $tr('Operational Priority') }}</span><StatusBadge :value="caseDetail.operationalPriority" /></div>
        <div><span>{{ $tr('Supply Alignment') }}</span><StatusBadge :value="alignment.status" /></div>
        <div class="next-action">
          <span>{{ $tr('Next Action') }} <small>{{ $tr('rule based') }}</small></span>
          <strong>{{ $tr(caseDetail.nextAction) }}</strong>
          <small class="next-action__reason">{{ $tr(caseDetail.nextActionReason) }}</small>
        </div>
      </div>
    </div>

    <PanelCard title="Customer Need / Supply Alignment" subtitle="Rule-based comparison. LLM is not used to decide supply feasibility.">
      <div class="alignment-grid">
        <div><span>{{ $tr('Customer Requested Date') }}</span><strong>{{ caseDetail.customerNeed.requestedDate }}</strong><small>{{ $tr(caseDetail.customerNeed.source) }}</small></div>
        <div><span>{{ $tr('Part Status') }}</span><StatusBadge :value="caseDetail.part.status" /><small>{{ caseDetail.part.no }}</small></div>
        <div><span>{{ $tr('Current ETA') }}</span><strong>{{ caseDetail.part.eta }}</strong><small>{{ $tr('Estimated, not physical receipt') }}</small></div>
        <div><span>{{ $tr('Current Appointment') }}</span><strong>{{ caseDetail.appointment.date }} · {{ caseDetail.appointment.time }}</strong><small>{{ caseDetail.appointment.technician }}</small></div>
        <div class="alignment-result">
          <span>{{ $tr('Alignment') }}</span>
          <StatusBadge :value="alignment.status" />
          <p>{{ $tr(alignment.reason) }}</p>
        </div>
      </div>
    </PanelCard>

    <PanelCard
      class="intelligence-panel"
      :title="aiT('Promise & Supply Intelligence', '약속 및 공급 인텔리전스')"
      :subtitle="aiT(
        'AI assists with semantic review. Rule-based states and database values are not changed by AI.',
        'AI는 의미 해석을 보조하며 규칙 기반 상태와 데이터베이스 값은 변경하지 않습니다.'
      )"
    >
      <template #actions>
        <button
          class="button button--secondary"
          :disabled="intelligenceLoading"
          @click="runCaseIntelligence"
        >
          {{
            intelligenceLoading
              ? aiT('Analyzing...', '분석 중...')
              : aiT('Run Case Intelligence', 'AI Case 검토')
          }}
        </button>
      </template>

      <div v-if="aiError" class="ai-intelligence__error">
        {{ aiError }}
      </div>

      <div class="ai-intelligence-grid">

        <!-- FEATURE 1 -->
        <section class="ai-feature">
          <div class="ai-feature__heading">
            <div>
              <small>{{ aiT('AI FEATURE 1', 'AI 기능 1') }}</small>
              <h4>{{ aiT('Promise Integrity', '고객 약속 정합성') }}</h4>
            </div>

            <span
              v-if="promiseIntegrityResult"
              class="ai-state"
              :class="aiStateClass(promiseIntegrityResult.ruleStatus)"
            >
              {{ aiStatusLabel(promiseIntegrityResult.ruleStatus) }}
            </span>
          </div>

          <template v-if="promiseIntegrityResult">
            <p class="ai-feature__primary">
              {{ aiPromisePrimary(promiseIntegrityResult) }}
            </p>

            <div class="ai-evidence">
              <span>{{ aiT('Evidence', '근거') }}</span>
              <p>{{ aiPromiseEvidence(promiseIntegrityResult) }}</p>
            </div>

            <small>
              {{ aiT('Part', '부품') }}
              {{ aiStatusLabel(promiseIntegrityResult.partStatus) }}
              · ETA {{ promiseIntegrityResult.eta || 'N/A' }}
            </small>
          </template>

          <p v-else class="ai-empty">
            {{
              aiT(
                'Run Case Intelligence to review customer commitments against current facts.',
                'AI Case 검토를 실행해 고객 약속과 현재 공급 사실의 정합성을 확인합니다.'
              )
            }}
          </p>
        </section>


        <!-- FEATURE 5 -->
        <section class="ai-feature">
          <div class="ai-feature__heading">
            <div>
              <small>{{ aiT('AI FEATURE 5', 'AI 기능 5') }}</small>
              <h4>{{ aiT('Data Consistency', '데이터 정합성') }}</h4>
            </div>

            <span
              v-if="contradictionResult"
              class="ai-state"
              :class="contradictionResult.hasPossibleConflict
                ? 'ai-state--danger'
                : 'ai-state--ok'"
            >
              {{
                contradictionResult.hasPossibleConflict
                  ? aiT('Possible conflict', '충돌 가능성')
                  : aiT('No conflict', '충돌 없음')
              }}
            </span>
          </div>

          <template v-if="contradictionResult">
            <p>{{ aiTimelineSummary(contradictionResult) }}</p>

            <div
              v-for="(conflict, index) in contradictionResult.conflicts"
              :key="index"
              class="ai-evidence"
            >
              <span>
                {{ conflict.sourceA }} ↔ {{ conflict.sourceB }}
              </span>

              <p>
                {{
                  locale === 'ko'
                    ? '현재 부품 상태와 타임라인 기록 사이에 의미상 충돌 가능성이 있어 직원 확인이 필요합니다.'
                    : conflict.reason
                }}
              </p>
            </div>

            <div
              v-if="!contradictionResult.hasPossibleConflict"
              class="ai-evidence"
            >
              <span>{{ aiT('Human-review flag', '직원 확인 결과') }}</span>
              <p>{{ aiConflictReview(contradictionResult) }}</p>
            </div>
          </template>

          <p v-else class="ai-empty">
            {{
              aiT(
                'Run Case Intelligence to compare Part state and Timeline meaning.',
                'AI Case 검토를 실행해 부품 상태와 타임라인 기록의 의미를 비교합니다.'
              )
            }}
          </p>
        </section>


        <!-- FEATURE 4 -->
        <section class="ai-feature">
          <div class="ai-feature__heading">
            <div>
              <small>{{ aiT('AI FEATURE 4', 'AI 기능 4') }}</small>
              <h4>
                {{ aiT('What Should I Verify First?', '무엇을 먼저 확인해야 하나요?') }}
              </h4>
            </div>
          </div>

          <template v-if="nextBestQuestionResult">
            <p class="ai-feature__primary">
              {{ aiVerifyPrimary(nextBestQuestionResult) }}
            </p>

            <p>{{ aiVerifyReason(nextBestQuestionResult) }}</p>

            <div
              v-if="nextBestQuestionResult.informationMissing"
              class="ai-tags"
            >
              <span
                v-for="field in nextBestQuestionResult.missingFields"
                :key="field"
              >
                {{ aiMissingFieldLabel(field) }}
              </span>
            </div>

            <small v-if="nextBestQuestionResult.informationMissing">
              {{
                aiT(
                  `AI explanation used: ${nextBestQuestionResult.llmUsed ? 'Yes' : 'No'}`,
                  `AI 설명 사용: ${nextBestQuestionResult.llmUsed ? '예' : '아니오'}`
                )
              }}
            </small>
          </template>

          <p v-else class="ai-empty">
            {{
              aiT(
                'Run Case Intelligence to check for information gaps.',
                'AI Case 검토를 실행해 누락된 정보를 확인합니다.'
              )
            }}
          </p>
        </section>


        <!-- FEATURE 3 -->
        <section class="ai-feature">
          <div class="ai-feature__heading">
            <div>
              <small>{{ aiT('AI FEATURE 3', 'AI 기능 3') }}</small>
              <h4>{{ aiT('Change Impact Radar', '변경 영향 분석') }}</h4>
            </div>
          </div>

          <label class="ai-input">
            <span>{{ aiT('Proposed Part ETA', '변경 예정 부품 ETA') }}</span>
            <input
              v-model="proposedEta"
              type="date"
            />
          </label>

          <button
            class="button button--secondary"
            :disabled="changeImpactLoading"
            @click="analyzeEtaChange"
          >
            {{
              changeImpactLoading
                ? aiT('Analyzing...', '분석 중...')
                : aiT('Analyze Change', '변경 영향 분석')
            }}
          </button>

          <template v-if="changeImpactResult">
            <div class="ai-transition">
              <div>
                <span>{{ aiT('Supply Alignment', '공급 충족 상태') }}</span>
                <strong>
                  {{ aiStatusLabel(changeImpactResult.supplyAlignmentBefore) }}
                  →
                  {{ aiStatusLabel(changeImpactResult.supplyAlignmentAfter) }}
                </strong>
              </div>

              <div>
                <span>{{ aiT('Promise Risk', '고객 약속 위험') }}</span>
                <strong>
                  {{ aiStatusLabel(changeImpactResult.promiseRiskBefore) }}
                  →
                  {{ aiStatusLabel(changeImpactResult.promiseRiskAfter) }}
                </strong>
              </div>
            </div>

            <p>{{ aiChangeSummary(changeImpactResult) }}</p>

            <div class="ai-evidence">
              <span>{{ aiT('Workflow impact', '업무 영향') }}</span>
              <p>{{ aiWorkflowImpact(changeImpactResult) }}</p>
            </div>

            <small>
              {{
                aiT(
                  `Appointment affected: ${changeImpactResult.affectedAppointment ? 'Yes' : 'No'}`,
                  `예약 영향: ${changeImpactResult.affectedAppointment ? '있음' : '없음'}`
                )
              }}
            </small>
          </template>
        </section>


        <!-- FEATURE 2 -->
        <section class="ai-feature ai-feature--wide">
          <div class="ai-feature__heading">
            <div>
              <small>{{ aiT('AI FEATURE 2', 'AI 기능 2') }}</small>
              <h4>
                {{ aiT('Customer Communication Guard', '고객 안내 문구 검토') }}
              </h4>
            </div>

            <span
              v-if="overpromiseResult"
              class="ai-state"
              :class="aiStateClass(overpromiseResult.risk)"
            >
              {{ aiStatusLabel(overpromiseResult.risk) }}
            </span>
          </div>

          <label class="ai-input">
            <span>
              {{ aiT('Draft message to customer', '고객 안내 초안') }}
            </span>

            <textarea
              v-model="overpromiseDraft"
              rows="3"
              :placeholder="aiT(
                'Example: Your part will arrive on Sep 2.',
                '예: 부품은 9월 2일에 도착할 예정입니다.'
              )"
            ></textarea>
          </label>

          <button
            class="button button--secondary"
            :disabled="overpromiseLoading"
            @click="checkOverpromiseDraft"
          >
            {{
              overpromiseLoading
                ? aiT('Checking...', '검토 중...')
                : aiT('Check Draft Before Sending', '발송 전 문구 검토')
            }}
          </button>

          <template v-if="overpromiseResult">
            <div class="ai-transition">
              <div>
                <span>{{ aiT('Communication Risk', '고객 안내 위험도') }}</span>
                <strong>{{ aiStatusLabel(overpromiseResult.risk) }}</strong>
              </div>

              <div>
                <span>{{ aiT('Supply Fact', '현재 공급 사실') }}</span>
                <strong>
                  {{ aiStatusLabel(overpromiseResult.partStatus) }}
                  · ETA {{ overpromiseResult.eta || 'N/A' }}
                </strong>
              </div>
            </div>

            <div
              v-if="overpromiseResult.unsupportedPhrase"
              class="ai-evidence"
            >
              <span>
                {{ aiT('Unsupported certainty', '근거보다 확정적인 표현') }}
              </span>

              <!-- 실제 검토 대상 문구이므로 원문 유지 -->
              <p>{{ overpromiseResult.unsupportedPhrase }}</p>
            </div>

            <div class="ai-evidence">
              <span>{{ aiT('Review', '검토 결과') }}</span>
              <p>{{ aiOverpromiseReason(overpromiseResult) }}</p>
            </div>

            <div
              v-if="overpromiseResult.saferWording"
              class="ai-evidence ai-evidence--suggestion"
            >
              <span>{{ aiT('Safer wording', '더 안전한 안내 문구') }}</span>

              <!-- 고객에게 보낼 실제 문구라 입력 언어 유지 -->
              <p>{{ overpromiseResult.saferWording }}</p>
            </div>
          </template>
        </section>

      </div>

      <p class="ai-disclaimer">
        {{
          aiT(
            'AI outputs are advisory and evidence-backed. They do not update Case Status, Supply Alignment, Next Action, or database records automatically.',
            'AI 결과는 근거 기반 참고 정보이며 Case 상태, 공급 충족 상태, 다음 조치 또는 데이터베이스 값을 자동으로 변경하지 않습니다.'
          )
        }}
      </p>
    </PanelCard>

    <div class="case-grid case-grid--after-alignment">
      <PanelCard title="Customer / Vehicle">
        <div class="info-grid">
          <div><span>{{ $tr('Customer No.') }}</span><strong>{{ caseDetail.customer.no }}</strong></div>
          <div><span>{{ $tr('Name') }}</span><strong>{{ caseDetail.customer.name }}</strong></div>
          <div><span>{{ $tr('Phone') }}</span><strong>{{ caseDetail.customer.phone }}</strong></div>
          <div><span>{{ $tr('Vehicle') }}</span><strong>{{ caseDetail.vehicle.model }}</strong></div>
          <div class="info-grid__wide"><span>{{ $tr('VIN') }}</span><strong class="mono">{{ caseDetail.vehicle.vin }}</strong></div>
          <div><span>{{ $tr('Plate') }}</span><strong>{{ caseDetail.vehicle.plate }}</strong></div>
        </div>
      </PanelCard>

      <PanelCard title="Recall Campaign">
        <div class="info-grid">
          <div><span>{{ $tr('Recall No.') }}</span><strong class="mono">{{ caseDetail.recall.no }}</strong></div>
          <div><span>{{ $tr('Severity') }}</span><StatusBadge :value="caseDetail.recall.severity" /></div>
          <div><span>{{ $tr('Status') }}</span><StatusBadge :value="caseDetail.recall.status" /></div>
          <div class="info-grid__wide"><span>{{ $tr('Safety Impact') }}</span><strong>{{ caseDetail.recall.safetyImpact }}</strong></div>
        </div>
      </PanelCard>

      <PanelCard title="Part / ETA">
        <template #actions>
          <button class="panel-link" @click="router.push('/parts')">{{ $tr(canEditPart ? 'Manage Part' : 'View Part') }}</button>
        </template>
        <div class="info-grid">
          <div><span>{{ $tr('Part No.') }}</span><strong class="mono">{{ caseDetail.part.no }}</strong></div>
          <div><span>{{ $tr('Status') }}</span><StatusBadge :value="caseDetail.part.status" /></div>
          <div><span>{{ $tr('Part Name') }}</span><strong>{{ caseDetail.part.name }}</strong></div>
          <div><span>{{ $tr('ETA') }}</span><strong>{{ caseDetail.part.eta }}</strong></div>
          <div><span>{{ $tr('Order No.') }}</span><strong class="mono">{{ caseDetail.part.orderNo }}</strong></div>
        </div>
      </PanelCard>

      <PanelCard title="Current Appointment">
        <template #actions>
          <button class="panel-link" @click="router.push('/appointments')">{{ $tr(canManageAppointment ? 'Manage Appointment' : 'View Appointment') }}</button>
        </template>
        <div class="info-grid">
          <div><span>{{ $tr('Date') }}</span><strong>{{ caseDetail.appointment.date }}</strong></div>
          <div><span>{{ $tr('Time') }}</span><strong>{{ caseDetail.appointment.time }}</strong></div>
          <div><span>{{ $tr('Technician') }}</span><strong>{{ caseDetail.appointment.technician }}</strong></div>
          <div><span>{{ $tr('Status') }}</span><StatusBadge :value="caseDetail.appointment.status" /></div>
          <div><span>{{ $tr('Part Readiness') }}</span><StatusBadge :value="caseDetail.appointment.readiness" /></div>
        </div>
      </PanelCard>

      <PanelCard title="Service Progress">
        <div class="service-progress">
          <div><span>{{ $tr('Assigned Technician') }}</span><strong>{{ caseDetail.service.technician }}</strong></div>
          <div>
            <span>{{ $tr('Latest Status') }}</span>
            <select v-if="editingService" v-model="serviceStatus" class="service-status-select">
              <option value="NOT_STARTED">{{ $tr('NOT_STARTED') }}</option><option value="CHECKED_IN">{{ $tr('CHECKED_IN') }}</option><option value="IN_PROGRESS">{{ $tr('IN_PROGRESS') }}</option><option value="WAITING">{{ $tr('WAITING') }}</option><option value="COMPLETED">{{ $tr('COMPLETED') }}</option>
            </select>
            <StatusBadge v-else :value="serviceStatus" />
          </div>
          <div><span>{{ $tr('Last Updated') }}</span><strong>{{ caseDetail.service.updatedAt }}</strong></div>
          <button v-if="canEditService && !editingService" class="button button--primary" @click="editingService=true">{{ $tr('Update Status') }}</button>
          <div v-if="canEditService && editingService" class="service-edit-actions">
            <button class="button button--ghost button--small" @click="editingService=false">{{ $tr('Cancel') }}</button>
            <button class="button button--primary button--small" @click="saveServiceStatus">{{ $tr('Save') }}</button>
          </div>
        </div>
      </PanelCard>

      <PanelCard title="Contact Eligibility">
        <template #actions>
          <button
            v-if="['APPOINTMENT_COORDINATOR','SERVICE_MANAGER'].includes(session.role)"
            class="panel-link"
            @click="editingEligibility = !editingEligibility"
          >
            {{ $tr(editingEligibility ? 'Done' : 'Edit Flags') }}
          </button>
        </template>
        <div class="eligibility-grid">
          <label><span>{{ $tr('Ownership Valid') }}</span><input v-model="ownership" type="checkbox" :disabled="!editingEligibility" /><strong>{{ $tr(ownership ? 'YES' : 'NO') }}</strong></label>
          <label><span>{{ $tr('Service Area Valid') }}</span><input v-model="serviceArea" type="checkbox" :disabled="!editingEligibility" /><strong>{{ $tr(serviceArea ? 'YES' : 'NO') }}</strong></label>
          <label><span>{{ $tr('Phone Validity') }}</span><input v-model="phoneValidity" type="checkbox" :disabled="!editingEligibility" /><strong>{{ $tr(phoneValidity ? 'VALID' : 'INVALID') }}</strong></label>
          <div class="calculated-eligibility"><span>{{ $tr('Recall Outreach Eligibility') }}</span><StatusBadge :value="ownership && serviceArea && phoneValidity ? 'ELIGIBLE' : 'NOT_ELIGIBLE'" /><small>{{ $tr('Calculated by system (AND)') }}</small></div>
        </div>
      </PanelCard>
    </div>

    <PanelCard title="Interaction Timeline / Memo" subtitle="Decision to action: customer follow-up and internal handoff are recorded against the case.">
      <template #actions>
        <button v-if="canLogCustomerContact" class="button button--secondary button--small" @click="openActivity('CONTACT')">{{ $tr('+ Log Customer Interaction') }}</button>
        <button class="button button--ghost button--small" @click="openActivity('MEMO')">{{ $tr('+ Memo') }}</button>
      </template>
      <div class="timeline">
        <div v-for="(activity, index) in activities" :key="`${activity.date}-${index}`" class="timeline-item">
          <div class="timeline-rail"><span></span></div>
          <div class="timeline-time">{{ activity.date }}</div>
          <div class="timeline-content">
            <div><StatusBadge :value="activity.type" /><strong>{{ activity.owner }}</strong></div>
            <p>{{ activity.summary }}</p>
          </div>
        </div>
      </div>
    </PanelCard>

    <div v-if="activityModalOpen" class="modal-backdrop" @click.self="activityModalOpen=false">
      <div class="modal-card">
        <div class="modal-card__header">
          <div><span>{{ $tr('CASE ACTIVITY') }}</span><h2>{{ $tr(activityMode === 'CONTACT' ? 'Log Customer Interaction' : 'Add Case Memo') }}</h2></div>
          <button @click="activityModalOpen=false">×</button>
        </div>
        <div class="side-form activity-form">
          <template v-if="activityMode === 'CONTACT'">
            <label class="field"><span>{{ $tr('Channel') }}</span><select v-model="contactChannel"><option value="Phone">{{ $tr('Phone') }}</option><option value="Email">{{ $tr('Email') }}</option><option value="In Person">{{ $tr('In Person') }}</option></select></label>
            <label class="field"><span>{{ $tr('Outcome') }}</span><select v-model="contactOutcome"><option value="Reached">{{ $tr('Reached') }}</option><option value="Voicemail">{{ $tr('Voicemail') }}</option><option value="No Answer">{{ $tr('No Answer') }}</option><option value="Customer Callback Requested">{{ $tr('Customer Callback Requested') }}</option></select></label>
            <label class="field"><span>{{ $tr('Callback Date (optional)') }}</span><input v-model="callbackDate" type="date" /></label>
          </template>
          <label class="field"><span>{{ $tr(activityMode === 'CONTACT' ? 'Interaction Summary' : 'Memo') }}</span><textarea v-model="activitySummary" rows="5" :placeholder="$tr('Record facts, decision and next handoff only.')"></textarea></label>
          <div v-if="feedback && activityModalOpen" class="employee-feedback employee-feedback--error">{{ feedback }}</div>
        </div>
        <div class="modal-card__footer">
          <button class="button button--ghost" @click="activityModalOpen=false">{{ $tr('Cancel') }}</button>
          <button class="button button--primary" @click="saveActivity">{{ $tr('Save Activity') }}</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.ai-intelligence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ai-feature {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--border-color, #d8e0e8);
  border-radius: 14px;
  background: var(--surface-color, #ffffff);
}

.ai-feature--wide {
  grid-column: 1 / -1;
}

.ai-feature__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.ai-feature__heading small {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0.55;
}

.ai-feature__heading h4 {
  margin: 0;
  font-size: 16px;
}

.ai-feature__primary {
  margin: 0 0 10px;
  font-weight: 700;
}

.ai-empty {
  margin: 0;
  opacity: 0.58;
  line-height: 1.5;
}

.ai-evidence {
  margin-top: 12px;
  padding: 11px 12px;
  border-radius: 10px;
  background: rgba(61, 92, 122, 0.07);
}

.ai-evidence span,
.ai-transition span,
.ai-input span {
  display: block;
  margin-bottom: 5px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  opacity: 0.62;
}

.ai-evidence p {
  margin: 0;
  line-height: 1.45;
}

.ai-evidence--suggestion {
  border-left: 3px solid #52789c;
}

.ai-state {
  display: inline-flex;
  align-items: center;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.ai-state--ok {
  background: rgba(55, 125, 86, 0.12);
}

.ai-state--danger {
  background: rgba(174, 64, 64, 0.12);
}

.ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 12px 0;
}

.ai-tags span {
  padding: 5px 8px;
  border: 1px solid var(--border-color, #d8e0e8);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.ai-input {
  display: block;
  margin-bottom: 10px;
}

.ai-input input,
.ai-input textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 11px;
  border: 1px solid var(--border-color, #ccd6e0);
  border-radius: 9px;
  background: #fff;
  font: inherit;
}

.ai-input textarea {
  resize: vertical;
}

.ai-transition {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.ai-transition > div {
  padding: 10px 11px;
  border-radius: 9px;
  background: rgba(61, 92, 122, 0.06);
}

.ai-transition strong {
  display: block;
  font-size: 13px;
}

.ai-intelligence__error {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 9px;
  background: rgba(174, 64, 64, 0.1);
  font-size: 13px;
  font-weight: 600;
}

.ai-disclaimer {
  margin: 16px 0 0;
  padding-top: 13px;
  border-top: 1px solid var(--border-color, #d8e0e8);
  font-size: 12px;
  line-height: 1.5;
  opacity: 0.62;
}

@media (max-width: 900px) {
  .ai-intelligence-grid,
  .ai-transition {
    grid-template-columns: 1fr;
  }

  .ai-feature--wide {
    grid-column: auto;
  }
}
</style>

<style scoped>
/* =========================================================
   RECALLRELAY — AI INTELLIGENCE CONSOLE
   ========================================================= */

.intelligence-panel {
  margin-top: 14px;
  border: 1px solid #a9c5d7;
  background:
    linear-gradient(
      180deg,
      #f4f9fc 0%,
      #eef5f9 100%
    );
  box-shadow:
    0 5px 18px rgba(25, 65, 90, 0.08),
    inset 0 4px 0 #2d668b;
}


/* ---------- AI TOP BANNER ---------- */

.ai-console-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;

  margin-bottom: 12px;
  padding: 13px 15px;

  border: 1px solid #c8dce8;
  border-radius: 7px;

  background:
    linear-gradient(
      90deg,
      #e8f2f8 0%,
      #f8fbfd 72%
    );
}

.ai-console-banner__main {
  min-width: 0;
}

.ai-console-banner__eyebrow {
  display: block;

  margin-bottom: 4px;

  color: #376782;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.ai-console-banner__main strong {
  display: block;

  margin-bottom: 3px;

  color: #153f55;
  font-size: 14px;
  font-weight: 750;
}

.ai-console-banner__main small {
  color: #6e8491;
  font-size: 11px;
  line-height: 1.45;
}

.ai-console-banner__chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;

  flex-shrink: 0;
}

.ai-console-banner__chips span {
  padding: 5px 7px;

  border: 1px solid #b7d0df;
  border-radius: 4px;

  background: rgba(255, 255, 255, 0.8);

  color: #3c657c;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.06em;
}


/* ---------- FEATURE GRID ---------- */

.ai-intelligence-grid {
  gap: 10px;
}


/* 기존 큰 둥근 카드 느낌 제거 */
.ai-feature {
  padding: 14px 15px;

  border: 1px solid #cfdee7;
  border-radius: 7px;

  background: rgba(255, 255, 255, 0.94);

  box-shadow:
    inset 3px 0 0 #83adc5,
    0 1px 3px rgba(27, 65, 87, 0.04);

  min-height: 0;
}


/* 핵심 판단 기능은 살짝 더 강조 */
.ai-feature:nth-child(1),
.ai-feature:nth-child(2) {
  box-shadow:
    inset 3px 0 0 #356f93,
    0 1px 4px rgba(27, 65, 87, 0.06);
}


/* ---------- FEATURE HEADER ---------- */

.ai-feature__heading {
  margin-bottom: 10px;
}

.ai-feature__heading small {
  display: inline-flex;

  width: fit-content;

  margin-bottom: 5px;
  padding: 3px 6px;

  border-radius: 3px;

  background: #e8f1f6;

  color: #52778b;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.09em;
  opacity: 1;
}

.ai-feature__heading h4 {
  color: #163f53;
  font-size: 14px;
  font-weight: 750;
}


/* ---------- EMPTY STATE ---------- */

.ai-empty {
  margin-top: 4px;

  color: #78909e;
  font-size: 12px;
  line-height: 1.45;
  opacity: 1;
}


/* ---------- RESULT ---------- */

.ai-feature__primary {
  color: #173f54;
  font-size: 13px;
}

.ai-feature > p {
  font-size: 12px;
  line-height: 1.5;
}


/* ---------- EVIDENCE ---------- */

.ai-evidence {
  margin-top: 9px;
  padding: 9px 10px;

  border: 1px solid #d9e5ec;
  border-radius: 5px;

  background: #f6f9fb;
}

.ai-evidence span {
  color: #597688;
  opacity: 1;
}

.ai-evidence p {
  color: #314e5e;
  font-size: 11px;
}


/* ---------- STATE PILLS ---------- */

.ai-state {
  border-radius: 4px;
  padding: 4px 7px;

  font-size: 9px;
  letter-spacing: 0.04em;
}

.ai-state--ok {
  border: 1px solid #bfddcf;
  background: #edf7f2;
  color: #32705a;
}

.ai-state--danger {
  border: 1px solid #ebc5c5;
  background: #fbefef;
  color: #984c4c;
}


/* ---------- INPUT ---------- */

.ai-input span {
  color: #587384;
  opacity: 1;
}

.ai-input input,
.ai-input textarea {
  border-radius: 5px;
  border-color: #c8d9e4;

  background: #fbfdfe;

  font-size: 12px;
}

.ai-input input:focus,
.ai-input textarea:focus {
  outline: none;

  border-color: #5988a6;

  box-shadow: 0 0 0 2px rgba(89, 136, 166, 0.11);
}


/* ---------- CHANGE RESULT ---------- */

.ai-transition {
  gap: 7px;
  margin-top: 10px;
}

.ai-transition > div {
  padding: 8px 9px;

  border: 1px solid #d9e5ec;
  border-radius: 5px;

  background: #f5f9fb;
}


/* ---------- BUTTONS INSIDE AI ---------- */

.intelligence-panel .ai-feature .button {
  min-height: 30px;
  padding: 6px 10px;

  border-radius: 5px;

  font-size: 10px;
  font-weight: 750;
}


/* ---------- DISCLAIMER ---------- */

.ai-disclaimer {
  margin-top: 11px;
  padding: 9px 11px;

  border: 1px solid #d6e3ea;
  border-radius: 5px;

  background: rgba(255, 255, 255, 0.65);

  color: #667f8e;
  font-size: 10px;
  opacity: 1;
}


/* ---------- ERROR ---------- */

.ai-intelligence__error {
  margin-bottom: 10px;

  border: 1px solid #ebc8c8;
  border-radius: 5px;

  background: #fbefef;

  color: #8e4949;
}


/* ---------- MOBILE ---------- */

@media (max-width: 900px) {
  .ai-console-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-console-banner__chips {
    justify-content: flex-start;
  }
}
</style>

<style scoped>
/* =========================================================
   AI INTELLIGENCE — COMPACT DASHBOARD OVERRIDE
   ========================================================= */

/* 전체 AI 영역을 더 촘촘하게 */
.intelligence-panel {
  margin-top: 12px;
}

/* 큰 소개 배너 → 얇은 상태 바 느낌 */
.ai-console-banner {
  min-height: 0;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 5px;
}

.ai-console-banner__eyebrow {
  margin-bottom: 2px;
  font-size: 8px;
}

.ai-console-banner__main strong {
  margin: 0;
  font-size: 12px;
}

/* 긴 설명은 이미 Panel subtitle과 disclaimer에 있으므로 숨김 */
.ai-console-banner__main small {
  display: none;
}

.ai-console-banner__chips {
  gap: 4px;
}

.ai-console-banner__chips span {
  padding: 3px 6px;
  font-size: 8px;
}


/* ---------- 3 COLUMN DASHBOARD ---------- */

.ai-intelligence-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

/*
1 = Promise
2 = Data Consistency
3 = Verify First
4 = Change Impact
5 = Communication Guard
*/

.ai-feature:nth-child(1) {
  grid-column: 1;
}

.ai-feature:nth-child(2) {
  grid-column: 2;
}

.ai-feature:nth-child(3) {
  grid-column: 3;
}

.ai-feature:nth-child(4) {
  grid-column: 1;
}

/* Communication Guard는 두 칸 사용 */
.ai-feature--wide {
  grid-column: 2 / 4;
}


/* ---------- CARD SIZE ---------- */

.ai-feature {
  min-height: auto;
  padding: 10px 12px;
  border-radius: 5px;
}

.ai-feature__heading {
  margin-bottom: 7px;
}

.ai-feature__heading small {
  margin-bottom: 3px;
  padding: 2px 5px;
  font-size: 8px;
}

.ai-feature__heading h4 {
  font-size: 12px;
}

.ai-empty {
  margin: 0;
  font-size: 10px;
  line-height: 1.35;
}


/* ---------- INPUT SIZE ---------- */

.ai-input {
  margin-bottom: 6px;
}

.ai-input span {
  margin-bottom: 3px;
  font-size: 9px;
}

.ai-input input {
  min-height: 32px;
  padding: 6px 8px;
  font-size: 10px;
}

.ai-input textarea {
  height: 58px;
  min-height: 58px;
  padding: 7px 8px;
  font-size: 10px;
}


/* ---------- BUTTON SIZE ---------- */

.intelligence-panel .ai-feature .button {
  min-height: 27px;
  padding: 4px 8px;
  font-size: 9px;
}


/* ---------- RESULT SIZE ---------- */

.ai-feature__primary {
  margin-bottom: 6px;
  font-size: 11px;
}

.ai-feature > p {
  margin: 5px 0;
  font-size: 10px;
  line-height: 1.35;
}

.ai-feature > small {
  font-size: 9px;
}

.ai-evidence {
  margin-top: 6px;
  padding: 6px 8px;
}

.ai-evidence span {
  margin-bottom: 2px;
  font-size: 8px;
}

.ai-evidence p {
  font-size: 9px;
  line-height: 1.35;
}

.ai-transition {
  gap: 5px;
  margin-top: 7px;
}

.ai-transition > div {
  padding: 6px 7px;
}

.ai-transition span {
  font-size: 8px;
}

.ai-transition strong {
  font-size: 10px;
}


/* ---------- DISCLAIMER ---------- */

.ai-disclaimer {
  margin-top: 7px;
  padding: 6px 8px;
  font-size: 9px;
}


/* ---------- RESPONSIVE ---------- */

@media (max-width: 1100px) {
  .ai-intelligence-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-feature:nth-child(1),
  .ai-feature:nth-child(2),
  .ai-feature:nth-child(3),
  .ai-feature:nth-child(4) {
    grid-column: auto;
  }

  .ai-feature--wide {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .ai-intelligence-grid {
    grid-template-columns: 1fr;
  }

  .ai-feature--wide {
    grid-column: auto;
  }
}
</style>
