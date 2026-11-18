const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'

async function request(path, options) {
  const res = await fetch(`${BASE}${path}`, options)
  const body = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(body.error || `Error ${res.status}`)
  return body
}

export const verificarContribuyente = (email, ruc) =>
  request('/api/contribuyentes/verificar', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, ruc }),
  })

export const obtenerVehiculo = (placa) =>
  request(`/api/vehiculos/${encodeURIComponent(placa)}`)

export const obtenerLicencia = (cedula) => request(`/api/licencias/${cedula}`)
