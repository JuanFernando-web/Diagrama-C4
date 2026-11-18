import { useState } from 'react'
import { verificarContribuyente, obtenerVehiculo, obtenerLicencia } from './api.js'

const ORIGEN = {
  ANT_EN_VIVO: { texto: 'Consultado en vivo a la ANT', clase: 'ok' },
  CACHE_FRESCO: { texto: 'Desde caché (dato reciente)', clase: 'info' },
  CACHE_RESPALDO: { texto: 'ANT no disponible: mostrando último dato guardado', clase: 'warn' },
}

function Datos({ obj }) {
  const item = Array.isArray(obj) ? obj[0] : obj
  if (!item) return null
  return (
    <dl>
      {Object.entries(item)
        .filter(([, v]) => v !== null && typeof v !== 'object' && v !== '')
        .map(([k, v]) => (
          <div key={k} className="row">
            <dt>{k}</dt>
            <dd>{String(v)}</dd>
          </div>
        ))}
    </dl>
  )
}

export default function App() {
  const [email, setEmail] = useState('')
  const [ruc, setRuc] = useState('')
  const [placa, setPlaca] = useState('')
  const [persona, setPersona] = useState(null)
  const [vehiculo, setVehiculo] = useState(null)
  const [licencia, setLicencia] = useState(null)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  async function ejecutar(fn) {
    setError('')
    setCargando(true)
    try {
      await fn()
    } catch (e) {
      setError(e.message)
    } finally {
      setCargando(false)
    }
  }

  const paso1 = (e) => {
    e.preventDefault()
    setPersona(null); setVehiculo(null); setLicencia(null)
    ejecutar(async () => setPersona(await verificarContribuyente(email, ruc)))
  }

  const paso2 = (e) => {
    e.preventDefault()
    setVehiculo(null); setLicencia(null)
    ejecutar(async () => {
      // Vehículo (SRI) y puntos de licencia (ANT con caché) en paralelo.
      const [v, l] = await Promise.allSettled([
        obtenerVehiculo(placa),
        obtenerLicencia(persona.cedula),
      ])
      if (v.status === 'fulfilled') setVehiculo(v.value)
      if (l.status === 'fulfilled') setLicencia(l.value)
      const fallo = [v, l].find((r) => r.status === 'rejected')
      if (fallo) setError(fallo.reason.message)
    })
  }

  return (
    <main>
      <h1>Consulta vehicular</h1>

      <form onSubmit={paso1} className="card">
        <h2>1. Contribuyente</h2>
        <label>Correo electrónico
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>RUC (persona natural)
          <input required inputMode="numeric" maxLength={13} pattern="\d{13}"
                 value={ruc} onChange={(e) => setRuc(e.target.value)} />
        </label>
        <button disabled={cargando}>Verificar</button>
      </form>

      {persona && (
        <section className="card">
          <h2>Datos del contribuyente</h2>
          <Datos obj={persona.datos} />
        </section>
      )}

      {persona && (
        <form onSubmit={paso2} className="card">
          <h2>2. Vehículo</h2>
          <label>Matrícula (placa, CAMV o CPN)
            <input required value={placa} onChange={(e) => setPlaca(e.target.value)} />
          </label>
          <button disabled={cargando}>Consultar vehículo y licencia</button>
        </form>
      )}

      {vehiculo && (
        <section className="card">
          <h2>Datos del vehículo</h2>
          <Datos obj={vehiculo} />
        </section>
      )}

      {licencia && (
        <section className="card">
          <h2>Puntos de la licencia</h2>
          <p className="puntos">{licencia.licencia.puntos}</p>
          <p className={`badge ${ORIGEN[licencia.origen].clase}`}>{ORIGEN[licencia.origen].texto}</p>
          <small>Última respuesta de la ANT: {new Date(licencia.consultadoEn).toLocaleString()}</small>
        </section>
      )}

      {cargando && <p className="cargando">Consultando…</p>}
      {error && <p className="error">{error}</p>}
    </main>
  )
}
