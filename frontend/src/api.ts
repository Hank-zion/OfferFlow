const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api'

export type ApiApplication = {
  id: number
  company: string
  position: string
  city?: string
  status: 'SAVED' | 'APPLIED' | 'WRITTEN_TEST' | 'INTERVIEWING' | 'OFFER' | 'REJECTED'
  appliedAt?: string
  source?: string
  note?: string
  salaryMin?: number
  salaryMax?: number
}
export type ApiInterview = {
  id: number
  applicationId: number
  roundNumber: number
  interviewedAt?: string
  result: 'PENDING' | 'PASSED' | 'FAILED'
  questions?: string
  reflection?: string
}

type PageResponse = { content: ApiApplication[]; totalPages: number }
type Credentials = { username: string; password: string }

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export function savedCredentials(): Credentials | null {
  const raw = sessionStorage.getItem('offerflow.credentials')
  if (!raw) return null
  try { return JSON.parse(raw) as Credentials } catch { return null }
}

export function saveCredentials(credentials: Credentials) {
  sessionStorage.setItem('offerflow.credentials', JSON.stringify(credentials))
}

export function clearCredentials() { sessionStorage.removeItem('offerflow.credentials') }

function headers(credentials = savedCredentials()) {
  const result: Record<string, string> = { 'Content-Type': 'application/json' }
  if (credentials) result.Authorization = `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`
  return result
}

async function request<T>(path: string, options: RequestInit = {}, credentials = savedCredentials()): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers: { ...headers(credentials), ...(options.headers ?? {}) } })
  if (!response.ok) throw new ApiError((await response.text()) || `Request failed: ${response.status}`, response.status)
  return response.status === 204 ? undefined as T : await response.json() as T
}

export async function me() { return request<{ username: string; email: string }>('/auth/me') }
export async function register(payload: { username: string; email: string; password: string }) { return request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }, null) }
export async function listApplications() {
  const pageSize = 100
  const firstPage = await request<PageResponse>(`/applications?page=0&size=${pageSize}`)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
      request<PageResponse>(`/applications?page=${index + 1}&size=${pageSize}`),
    ),
  )
  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}
export async function createApplication(payload: Omit<ApiApplication, 'id'>) { return request<ApiApplication>('/applications', { method: 'POST', body: JSON.stringify(payload) }) }
export async function updateApplication(application: ApiApplication) { return request<ApiApplication>(`/applications/${application.id}`, { method: 'PUT', body: JSON.stringify(application) }) }
export async function deleteApplication(id: number) { return request<void>(`/applications/${id}`, { method: 'DELETE' }) }
export async function dashboard() { return request<{ total: number; byStatus: Record<string, number>; offerConversionRate: number }>('/dashboard') }
export async function listInterviews(applicationId: number) { return request<ApiInterview[]>(`/applications/${applicationId}/interviews`) }
export async function createInterview(applicationId: number, payload: Omit<ApiInterview, 'id' | 'applicationId'>) { return request<ApiInterview>(`/applications/${applicationId}/interviews`, { method: 'POST', body: JSON.stringify(payload) }) }
export async function updateInterview(applicationId: number, interview: ApiInterview) { return request<ApiInterview>(`/applications/${applicationId}/interviews/${interview.id}`, { method: 'PUT', body: JSON.stringify(interview) }) }
export async function deleteInterview(applicationId: number, interviewId: number) { return request<void>(`/applications/${applicationId}/interviews/${interviewId}`, { method: 'DELETE' }) }
