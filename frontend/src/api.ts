export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export interface HealthResponse {
  status: string
}

export async function getBackendHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_URL}/actuator/health`)
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  return response.json()
}
