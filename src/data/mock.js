export const dashboardProfiles = {
  APPOINTMENT_COORDINATOR: {
    title: 'Appointment Coordinator Dashboard',
    eyebrow: 'Outbound recall coordination',
    stats: [
      ['High Priority', 12, 'P1 cases'],
      ['Contact Required', 18, 'eligible'],
      ['Callback Due', 7, 'today'],
      ['Appointment Required', 9, 'ready to book'],
      ['Part Arrived / Not Contacted', 6, 'action needed'],
    ],
    primaryTitle: 'Action Required Queue',
    primaryColumns: ['Priority', 'Due', 'Customer / VIN', 'Recall', 'Reason', 'Next Action'],
    primaryRows: [
      ['P1', 'Sep 02', 'Amy Henderson · ...0001', 'WXR-2026-14', 'Part still in transit', 'VERIFY PART STATUS'],
      ['P1', 'Today 13:00', 'Michael Chen · ...8821', 'WRL-2026-03', 'ETA unavailable', 'CONTACT CUSTOMER'],
      ['P2', 'Tomorrow', 'Olivia Martin · ...1120', 'WXR-2026-14', 'Appointment review', 'REVIEW APPOINTMENT'],
      ['P2', 'Aug 21', 'Ethan Walker · ...6410', 'RCL-26-118', 'Outreach due', 'CONTACT CUSTOMER'],
      ['P3', 'Aug 22', 'Sophia Kim · ...7902', 'RCL-26-118', 'Part status stale', 'CHECK PART STATUS'],
    ],
    secondary: [
      {
        title: 'Upcoming Appointments',
        columns: ['Time', 'Customer / VIN', 'Recall', 'Part Readiness'],
        rows: [
          ['09:00', 'Noah Davis · ...1941', 'WXR-2026-14', 'READY'],
          ['10:30', 'Emma Wilson · ...4270', 'RCL-26-118', 'NOT_READY'],
          ['13:20', 'Lucas Moore · ...3408', 'WRL-2026-03', 'READY'],
        ],
      },
      {
        title: 'Recent Contacts',
        columns: ['Customer', 'Channel', 'Outcome', 'Next Action'],
        rows: [
          ['A. Henderson', 'Phone', 'Reached', 'VERIFY PART STATUS'],
          ['M. Chen', 'Phone', 'Reached', 'APPOINTMENT'],
          ['S. Kim', 'Email', 'Sent', 'WAIT'],
        ],
      },
    ],
  },

  PARTS_SPECIALIST: {
    title: 'Parts Specialist Dashboard',
    eyebrow: 'Recall parts & ETA control',
    stats: [
      ['Requested / Not Ordered', 14, 'needs PO'],
      ['Backordered', 8, 'supplier hold'],
      ['ETA Passed', 5, 'overdue'],
      ['In Transit', 21, 'tracking'],
      ['Arrived Today', 9, 'received'],
    ],
    primaryTitle: 'Parts Action Queue',
    primaryColumns: ['Priority', 'Part No.', 'Recall', 'Customer / VIN', 'Status', 'ETA'],
    primaryRows: [
      ['P1', 'PR-10024', 'WXR-2026-14', 'A. Henderson · ...0001', 'IN_TRANSIT', '2026-09-02'],
      ['P1', 'FP-44102', 'WRL-2026-03', 'M. Chen · ...8821', 'BACKORDERED', 'N/A'],
      ['P2', 'BKT-203', 'RCL-26-118', 'O. Martin · ...1120', 'ORDERED', '2026-08-23'],
      ['P2', 'PR-10024', 'WXR-2026-14', 'N. Davis · ...1941', 'ARRIVED', '2026-08-19'],
    ],
    secondary: [
      {
        title: 'Arrivals Today',
        columns: ['Part No.', 'Recall', 'Customer / VIN', 'Received'],
        rows: [
          ['PR-10024', 'WXR-2026-14', 'N. Davis · ...1941', '08:42'],
          ['FP-44102', 'WRL-2026-03', 'L. Moore · ...3408', '09:15'],
          ['BKT-203', 'RCL-26-118', 'S. Kim · ...7902', '11:03'],
        ],
      },
      {
        title: 'Recent Part Updates',
        columns: ['Part', 'Change', 'By', 'Time'],
        rows: [
          ['PR-10024', 'IN_TRANSIT', 'Jordan P.', '11:22'],
          ['FP-44102', 'ETA → N/A', 'Jordan P.', '10:51'],
          ['BKT-203', 'ARRIVED', 'Jordan P.', '09:12'],
        ],
      },
    ],
  },

  SERVICE_ADVISOR: {
    title: 'Service Advisor Dashboard',
    eyebrow: 'Inbound customer & service progress',
    stats: [
      ["Today's Appointments", 16, 'scheduled'],
      ['Checked In', 5, 'onsite'],
      ['In Progress', 4, 'technician'],
      ['Waiting', 3, 'paused'],
      ['Part Not Ready', 2, 'review'],
    ],
    primaryTitle: 'Current Service Queue',
    primaryColumns: ['Customer / VIN', 'Recall', 'Technician', 'Service Status', 'Last Updated'],
    primaryRows: [
      ['Amy Henderson · ...0001', 'WXR-2026-14', 'K. Adams', 'NOT_STARTED', '15:20'],
      ['Michael Chen · ...8821', 'WRL-2026-03', 'R. Patel', 'IN_PROGRESS', '15:14'],
      ['Emma Wilson · ...4270', 'RCL-26-118', 'K. Adams', 'CHECKED_IN', '14:58'],
      ['Lucas Moore · ...3408', 'WXR-2026-14', 'J. Brown', 'SCHEDULED', '14:32'],
    ],
    secondary: [
      {
        title: 'Upcoming Appointments',
        columns: ['Time', 'Customer', 'Part', 'Technician'],
        rows: [
          ['16:00', 'Noah Davis', 'READY', 'K. Adams'],
          ['16:30', 'Sophia Kim', 'NOT_READY', 'R. Patel'],
          ['17:10', 'Ethan Walker', 'READY', 'J. Brown'],
        ],
      },
      {
        title: 'Follow-up Required',
        columns: ['Case', 'Reason', 'Next Action', 'Due'],
        rows: [
          ['RC-20881', 'Part still in transit', 'VERIFY PART STATUS', 'Sep 02'],
          ['RC-20876', 'Waiting service', 'REVIEW CASE', 'Now'],
          ['RC-20854', 'Additional service', 'HANDOFF', 'Today'],
        ],
      },
    ],
  },

  TECHNICIAN: {
    title: 'Technician Dashboard',
    eyebrow: 'Assigned recall jobs',
    stats: [
      ["Today's Jobs", 9, 'assigned'],
      ['Checked In', 2, 'ready'],
      ['In Progress', 1, 'active'],
      ['Waiting', 1, 'blocked'],
      ['Completed', 5, 'today'],
    ],
    primaryTitle: 'Assigned Recall Jobs',
    primaryColumns: ['Case / VIN', 'Recall', 'Part Ready', 'Service Status', 'Action'],
    primaryRows: [
      ['RC-20881 · ...0001', 'WXR-2026-14', 'NOT_READY', 'NOT_STARTED', 'View'],
      ['RC-20879 · ...8821', 'WRL-2026-03', 'READY', 'CHECKED_IN', 'Start Service'],
      ['RC-20866 · ...4270', 'RCL-26-118', 'READY', 'SCHEDULED', 'Check In'],
      ['RC-20852 · ...3408', 'WXR-2026-14', 'READY', 'COMPLETED', 'View'],
    ],
    secondary: [
      {
        title: 'Upcoming Jobs',
        columns: ['Time', 'Case / VIN', 'Recall', 'Part Ready'],
        rows: [
          ['16:00', 'RC-20895 · ...1941', 'WXR-2026-14', 'READY'],
          ['16:40', 'RC-20901 · ...7902', 'RCL-26-118', 'READY'],
          ['Tomorrow', 'RC-20918 · ...6410', 'WRL-2026-03', 'READY'],
        ],
      },
      {
        title: 'Case Memo',
        columns: ['Case', 'Customer', 'Memo', 'Time'],
        rows: [
          ['RC-20881', 'A. Henderson', 'Verify part arrival before Sep 3 service', '15:11'],
          ['RC-20879', 'M. Chen', 'Check relay connector', '14:47'],
          ['RC-20866', 'E. Wilson', 'Loaner requested', '14:19'],
        ],
      },
    ],
  },

  SERVICE_MANAGER: {
    title: 'Service Manager Dashboard',
    eyebrow: 'Dealership recall operations',
    stats: [
      ['P1 Priority Cases', 18, 'open'],
      ['Delayed Cases', 7, '> 48h'],
      ['Pending Actions', 26, 'team queue'],
      ['ETA Passed', 5, 'parts'],
      ['Waiting Service', 4, 'shop floor'],
    ],
    primaryTitle: 'Priority / Delayed Cases',
    primaryColumns: ['Priority', 'Customer / VIN', 'Recall', 'Customer Need', 'Supply', 'Risk', 'Next Action'],
    primaryRows: [
      ['P1', 'Amy Henderson · ...0001', 'WXR-2026-14', 'Sep 03', 'ETA Sep 02', { value: 'WATCH', badge: true, subtext: 'In transit; arrival not yet confirmed' }, 'VERIFY PART STATUS'],
      ['P1', 'Michael Chen · ...8821', 'WRL-2026-03', 'ASAP', 'ETA N/A', { value: 'AT_RISK', badge: true, subtext: 'ASAP request with no confirmed ETA' }, 'CHECK PART STATUS'],
      ['P1', 'Emma Wilson · ...4270', 'RCL-26-118', 'Aug 22', 'ETA Aug 23', { value: 'AT_RISK', badge: true, subtext: 'ETA is later than customer need' }, 'CONTACT CUSTOMER'],
      ['P2', 'Lucas Moore · ...3408', 'WXR-2026-14', 'Aug 24', 'ARRIVED', { value: 'ALIGNED', badge: true, subtext: 'Part physically received' }, 'REVIEW APPOINTMENT'],
      ['P2', 'Sophia Kim · ...7902', 'RCL-26-118', 'Aug 22', 'ETA Aug 21', { value: 'WATCH', badge: true, subtext: 'ETA is on time; receipt still unconfirmed' }, 'VERIFY PART STATUS'],
    ],
    secondary: [
      {
        title: 'Team Workload',
        columns: ['Role', 'Open', 'Delayed'],
        rows: [
          ['Appointment Coordinator', '42', '5'],
          ['Parts Specialist', '31', '7'],
          ['Service Advisor', '18', '3'],
          ['Technician', '11', '1'],
        ],
      },
      {
        title: 'Campaign Summary',
        columns: ['Campaign', 'Priority', 'Open Cases'],
        rows: [
          ['WXR-2026-14', 'P1', '18'],
          ['WRL-2026-03', 'P2', '24'],
          ['RCL-26-118', 'P2', '11'],
        ],
      },
    ],
  },

  HEAD_OFFICE_ADMIN: {
    title: 'Head Office Dashboard',
    eyebrow: 'Organization-wide recall oversight',
    stats: [
      ['Active Campaigns', 12, 'organization'],
      ['Open Cases', 842, 'all dealerships'],
      ['In Progress', 118, 'service'],
      ['Completed', 1264, 'YTD'],
      ['Dealerships', 18, 'active'],
    ],
    primaryTitle: 'Dealership Recall Status',
    primaryColumns: ['Dealership', 'Campaigns', 'Open', 'In Progress', 'Completed', 'Attention'],
    primaryRows: [
      ['Downtown Subaru', '7', '64', '13', '211', '3'],
      ['Northside Subaru', '6', '48', '8', '184', '1'],
      ['Riverside Subaru', '8', '73', '17', '229', '5'],
      ['Airport Subaru', '5', '41', '6', '139', '0'],
      ['Central Subaru', '7', '56', '11', '202', '2'],
    ],
    secondary: [
      {
        title: 'Campaign Summary',
        columns: ['Campaign', 'Severity', 'Open', 'Completed'],
        rows: [
          ['WXR-2026-14', 'HIGH', '218', '482'],
          ['WRL-2026-03', 'MEDIUM', '124', '311'],
          ['RCL-26-118', 'HIGH', '186', '274'],
        ],
      },
      {
        title: 'Attention Required',
        columns: ['Dealership', 'Open Cases', 'Attention'],
        rows: [
          ['Riverside Subaru', '73', '5'],
          ['Downtown Subaru', '64', '3'],
          ['Central Subaru', '56', '2'],
        ],
      },
    ],
  },
}

dashboardProfiles.HEAD_OFFICE_MANAGER = {
  ...dashboardProfiles.HEAD_OFFICE_ADMIN,
  title: 'Head Office Manager Dashboard',
  eyebrow: 'Head Office recall operations',
}

dashboardProfiles.HEAD_OFFICE_STAFF = {
  ...dashboardProfiles.HEAD_OFFICE_ADMIN,
  title: 'Head Office Staff Dashboard',
  eyebrow: 'Organization recall support',
}

dashboardProfiles.CALL_CENTER_AGENT = {
  ...dashboardProfiles.APPOINTMENT_COORDINATOR,
  title: 'Call Center Dashboard',
  eyebrow: 'Customer recall outreach',
}

export const searchRows = [
  { id: 20881, dealership: 'Downtown Subaru', customer: 'Amy Henderson', vin: 'JF2BTABC1M000001', recall: 'WXR-2026-14', priority: 'P1', caseStatus: 'APPOINTMENT_SCHEDULED', partStatus: 'IN_TRANSIT', supplyRisk: 'WATCH', supplyRiskReason: 'ETA is before customer need, but the part is still in transit.' },
  { id: 20879, dealership: 'Downtown Subaru', customer: 'Michael Chen', vin: 'JF1SKABC9N0008821', recall: 'WRL-2026-03', priority: 'P1', caseStatus: 'CUSTOMER_CONTACTED', partStatus: 'BACKORDERED', supplyRisk: 'AT_RISK', supplyRiskReason: 'ASAP request has no confirmed ETA.' },
  { id: 20866, dealership: 'Downtown Subaru', customer: 'Emma Wilson', vin: '4S4BTABC0N0004270', recall: 'RCL-26-118', priority: 'P2', caseStatus: 'NEW', partStatus: 'ORDERED', supplyRisk: 'AT_RISK', supplyRiskReason: 'Part ETA is later than the customer requested date.' },
  { id: 20852, dealership: 'Downtown Subaru', customer: 'Lucas Moore', vin: 'JF2SKABC8M0003408', recall: 'WXR-2026-14', priority: 'P2', caseStatus: 'APPOINTMENT_SCHEDULED', partStatus: 'ARRIVED', supplyRisk: 'ALIGNED', supplyRiskReason: 'Required part has physically arrived.' },
  { id: 20845, dealership: 'Downtown Subaru', customer: 'Sophia Kim', vin: '4S3BWABC4L0007902', recall: 'RCL-26-118', priority: 'P3', caseStatus: 'CUSTOMER_CONTACTED', partStatus: 'IN_TRANSIT', supplyRisk: 'WATCH', supplyRiskReason: 'ETA is on time, but physical receipt is not confirmed.' },
]

export const caseDetail = {
  id: 20881,
  organization: 'Northstar Automotive Group',
  dealership: 'Downtown Subaru',
  caseStatus: 'APPOINTMENT_SCHEDULED',
  operationalPriority: 'P1',
  nextAction: 'VERIFY PART STATUS',
  nextActionReason: 'Part is still in transit. Confirm the latest ETA and physical arrival before the scheduled service date.',
  customerNeed: { requestedDate: '2026-09-03', source: 'Customer confirmed during outbound contact' },
  customer: { no: 'CUST-000123', name: 'Amy Henderson', phone: '(555) 123-8891' },
  vehicle: { vin: 'JF2BTABC1M000001', model: '2022 Subaru Outback', plate: 'BC 4K8-21P' },
  recall: { no: 'WXR-2026-14', severity: 'HIGH', status: 'ACTIVE', safetyImpact: 'Engine stall risk' },
  part: { no: 'PR-10024', name: 'Fuel Pump Relay', status: 'IN_TRANSIT', eta: '2026-09-02', orderNo: 'PO-882104' },
  appointment: { date: '2026-09-03', time: '10:00', technician: 'Kevin Adams', status: 'SCHEDULED', readiness: 'NOT_READY' },
  service: { status: 'NOT_STARTED', updatedAt: '2026-08-19 15:20', technician: 'Kevin Adams' },
  eligibility: { ownership: true, serviceArea: true, phoneValidity: true, outreach: 'ELIGIBLE' },
  activities: [
    { date: 'Aug 19 · 15:20', type: 'MEMO', owner: 'Kevin Adams', summary: 'Part tracking checked. Customer updated on current ETA.' },
    { date: 'Aug 19 · 11:40', type: 'CONTACT', owner: 'Mina Park', summary: 'Outbound call. Customer confirmed Sep 3 appointment.' },
    { date: 'Aug 18 · 16:05', type: 'CONTACT', owner: 'Mina Park', summary: 'Voicemail left. Callback due next morning.' },
  ],
}

export const partRows = [
  { id: 1, caseId: 20881, priority: 'P1', partNo: 'PR-10024', recall: 'WXR-2026-14', customer: 'Amy Henderson · ...0001', status: 'IN_TRANSIT', orderDate: '2026-08-12', orderNo: 'PO-882104', eta: '2026-09-02', needBy: '2026-09-03', supplyRisk: 'WATCH' },
  { id: 2, caseId: 20879, priority: 'P1', partNo: 'FP-44102', recall: 'WRL-2026-03', customer: 'Michael Chen · ...8821', status: 'BACKORDERED', orderDate: '2026-08-08', orderNo: 'PO-881902', eta: 'N/A', needBy: 'ASAP', supplyRisk: 'AT_RISK' },
  { id: 3, caseId: 20866, priority: 'P2', partNo: 'BKT-203', recall: 'RCL-26-118', customer: 'Emma Wilson · ...4270', status: 'ORDERED', orderDate: '2026-08-17', orderNo: 'PO-882231', eta: '2026-08-23', needBy: '2026-08-22', supplyRisk: 'AT_RISK' },
  { id: 4, caseId: 20852, priority: 'P2', partNo: 'PR-10024', recall: 'WXR-2026-14', customer: 'Lucas Moore · ...3408', status: 'ARRIVED', orderDate: '2026-08-10', orderNo: 'PO-881772', eta: '2026-08-19', needBy: '2026-08-24', supplyRisk: 'ALIGNED' },
  { id: 5, caseId: 20845, priority: 'P3', partNo: 'FP-44102', recall: 'WRL-2026-03', customer: 'Sophia Kim · ...7902', status: 'IN_TRANSIT', orderDate: '2026-08-11', orderNo: 'PO-881944', eta: '2026-08-21', needBy: '2026-08-22', supplyRisk: 'WATCH' },
]

export const appointmentRows = [
  { id: 501, time: '09:00', customer: 'Noah Davis', recall: 'WXR-2026-14', readiness: 'READY', technician: 'Kevin Adams', status: 'SCHEDULED' },
  { id: 502, time: '10:30', customer: 'Emma Wilson', recall: 'RCL-26-118', readiness: 'NOT_READY', technician: 'Ravi Patel', status: 'SCHEDULED' },
  { id: 503, time: '13:20', customer: 'Lucas Moore', recall: 'WRL-2026-03', readiness: 'READY', technician: 'James Brown', status: 'SCHEDULED' },
  { id: 504, time: '15:10', customer: 'Sophia Kim', recall: 'WXR-2026-14', readiness: 'READY', technician: 'Kevin Adams', status: 'SCHEDULED' },
]

export const campaignRows = [
  { id: 301, recallNo: 'WXR-2026-14', status: 'ACTIVE', severity: 'HIGH', type: 'Safety Recall', date: '2026-08-01', open: 218, dealershipOpen: 18, priority: 'P1', safetyImpact: 'Engine stall risk' },
  { id: 302, recallNo: 'WRL-2026-03', status: 'ACTIVE', severity: 'MEDIUM', type: 'Compliance Recall', date: '2026-07-12', open: 124, dealershipOpen: 24, priority: 'P2', safetyImpact: 'Compliance inspection required' },
  { id: 303, recallNo: 'RCL-26-118', status: 'ACTIVE', severity: 'HIGH', type: 'Safety Recall', date: '2026-06-21', open: 186, dealershipOpen: 11, priority: 'P2', safetyImpact: 'Potential brake hose degradation' },
  { id: 304, recallNo: 'SRV-2026-09', status: 'CLOSED', severity: 'LOW', type: 'Service Campaign', date: '2026-04-05', open: 0, dealershipOpen: 0, priority: 'P3', safetyImpact: 'Software service update' },
]

export const employeeRows = [
  { id: 90, login: 'hadmin', name: 'Helen Admin', email: 'admin@northstar.example', dealershipId: null, dealership: 'Head Office', department: 'HEAD_OFFICE', role: 'HEAD_OFFICE_ADMIN', active: true },
  { id: 91, login: 'homgr', name: 'Morgan Hill', email: 'homgr@northstar.example', dealershipId: null, dealership: 'Head Office', department: 'HEAD_OFFICE', role: 'HEAD_OFFICE_MANAGER', active: true },
  { id: 92, login: 'hostaff', name: 'Jamie Park', email: 'jamie.park@northstar.example', dealershipId: null, dealership: 'Head Office', department: 'HEAD_OFFICE', role: 'HEAD_OFFICE_STAFF', active: true },
  { id: 93, login: 'call01', name: 'Taylor Kim', email: 'taylor.kim@northstar.example', dealershipId: null, dealership: 'Head Office', department: 'HEAD_OFFICE', role: 'CALL_CENTER_AGENT', active: true },
  { id: 101, login: 'mpark', name: 'Mina Park', email: 'mina.park@northstar.example', dealershipId: 12, dealership: 'Downtown Subaru', department: 'SERVICE', role: 'APPOINTMENT_COORDINATOR', active: true },
  { id: 102, login: 'jporter', name: 'Jordan Porter', email: 'jordan.porter@northstar.example', dealershipId: 12, dealership: 'Downtown Subaru', department: 'PARTS', role: 'PARTS_SPECIALIST', active: true },
  { id: 103, login: 'slee', name: 'Sarah Lee', email: 'sarah.lee@northstar.example', dealershipId: 12, dealership: 'Downtown Subaru', department: 'SERVICE', role: 'SERVICE_ADVISOR', active: true },
  { id: 104, login: 'kadams', name: 'Kevin Adams', email: 'kevin.adams@northstar.example', dealershipId: 12, dealership: 'Downtown Subaru', department: 'SERVICE', role: 'TECHNICIAN', active: true },
  { id: 105, login: 'jlee', name: 'Celine Lee', email: 'celine.lee@northstar.example', dealershipId: 12, dealership: 'Downtown Subaru', department: 'SERVICE', role: 'SERVICE_MANAGER', active: true },
]
