<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'

const router = useRouter()
const name = ref('')
const phone = ref('')
const vin = ref('')
const checked = ref(false)
const created = ref(false)
const vinValid = computed(() => vin.value.length === 17)

function checkVin() {
  checked.value = vinValid.value
}
function createProfile() {
  if (!name.value || !phone.value || !vinValid.value) return
  created.value = true
  setTimeout(() => router.push('/cases/20881'), 650)
}
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="New-to-dealership customer" title="New Customer Registration" description="Create the minimum customer profile and connect VIN-matched recall case(s).">
      <button class="button button--secondary" @click="router.push('/search')">{{ $tr('Cancel') }}</button>
    </PageHeader>

    <div class="form-layout">
      <PanelCard title="Customer & Vehicle" subtitle="Name, phone and 17-character VIN are required." padded>
        <div class="form-grid">
          <label class="field"><span>{{ $tr('Customer Name') }}</span><input v-model="name" :placeholder="$tr('Full name')" /></label>
          <label class="field"><span>{{ $tr('Phone Number') }}</span><input v-model="phone" :placeholder="$tr('(555) 000-0000')" /></label>
          <label class="field field--wide">
            <span>{{ $tr('VIN') }} <small>{{ $tr('17 characters') }}</small></span>
            <div class="field-with-action">
              <input v-model="vin" class="mono" maxlength="17" :placeholder="$tr('JF2BTABC1M000001')" />
              <button class="button button--secondary" :disabled="!vinValid" @click="checkVin">{{ $tr('Check VIN / Recall') }}</button>
            </div>
          </label>
        </div>

        <div v-if="vin && !vinValid" class="notice notice--warning">{{ $tr('VIN must be exactly 17 characters before recall matching.') }}</div>
      </PanelCard>

      <PanelCard title="Recall Match Result" subtitle="The backend creates or links one recall case per matched campaign." padded>
        <div v-if="checked" class="recall-match">
          <div class="recall-match__status">{{ $tr('MATCH FOUND') }}</div>
          <div><span>{{ $tr('Recall No.') }}</span><strong>{{ $tr('WXR-2026-14') }}</strong></div>
          <div><span>{{ $tr('Campaign') }}</span><strong>{{ $tr('Fuel Pump Relay Safety Recall') }}</strong></div>
          <div><span>{{ $tr('Severity') }}</span><strong>{{ $tr('HIGH') }}</strong></div>
          <div><span>{{ $tr('Case Link') }}</span><strong>{{ $tr('New case will be created') }}</strong></div>
        </div>
        <div v-else class="empty-card">{{ $tr('Enter a valid VIN and run Check VIN / Recall.') }}</div>
      </PanelCard>
    </div>

    <div class="form-footer">
      <div><strong>{{ $tr('Assigned Dealership') }}</strong><span>{{ $tr('Downtown Subaru') }}</span></div>
      <button class="button button--primary" :disabled="!checked || !name || !phone" @click="createProfile">
        {{ $tr(created ? 'Creating…' : 'Create Profile & Recall Case(s)') }}
      </button>
    </div>
  </AppShell>
</template>
