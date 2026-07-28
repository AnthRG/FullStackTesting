import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { assignRole, listRoles, listUsers, removeRole } from '../adminApi'
import type { Role, UserWithRoles } from '../adminApi'
import Layout from '../components/Layout'
import { getFreshToken } from '../auth/keycloak'

// El gating de esta página es solo UX: oculta controles según el token.
// La protección real está en el backend, donde UserRolesController exige user:manage
// en los cinco endpoints de /api/admin/** (ver AuthorizationIT).


function IconLock() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.75}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" />
    </svg>
  )
}

function IconX({ className = 'w-3 h-3' }: { className?: string }) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
    </svg>
  )
}

// Keycloak devuelve en la misma lista los roles de negocio (INVENTORY_ADMIN) y los
// permisos que esos roles agrupan (product:view). Separarlos es lo que vuelve legible
// la pantalla: los primeros son la decisión, los segundos su consecuencia.
function isBusinessRole(role: string): boolean {
  return !role.includes(':')
}

function moduleOf(permission: string): string {
  return permission.split(':')[0]
}

const MODULE_CHIP: Record<string, string> = {
  product: 'bg-violet-50 text-violet-700',
  stock: 'bg-emerald-50 text-emerald-700',
  report: 'bg-sky-50 text-sky-700',
  audit: 'bg-orange-50 text-orange-700',
  user: 'bg-rose-50 text-rose-700',
}

function roleChipClasses(role: string): string {
  if (isBusinessRole(role)) return 'bg-blue-100 text-blue-800 font-semibold'
  return MODULE_CHIP[moduleOf(role)] ?? 'bg-amber-50 text-amber-700'
}

// Roles alfabéticos; permisos agrupados por módulo y alfabéticos dentro del módulo,
// para que la misma persona se vea siempre igual entre recargas.
function splitRoles(roles: string[]): { business: string[]; permissions: string[] } {
  const business = roles.filter(isBusinessRole).sort((a, b) => a.localeCompare(b))
  const permissions = roles.filter(r => !isBusinessRole(r)).sort((a, b) => a.localeCompare(b))
  return { business, permissions }
}

export default function UsersPage() {
  const { user, logout, hasPermission } = useAuth()
  const navigate = useNavigate()

  // Los cinco endpoints de /api/admin/** exigen el mismo permiso, asi que ver y editar
  // no se separan: quien puede entrar puede administrar.
  const canView = hasPermission('user:manage')
  const canEdit = canView

  const [users, setUsers] = useState<UserWithRoles[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [filter, setFilter] = useState('')

  const [confirmRemove, setConfirmRemove] = useState<{ userId: string; role: string } | null>(null)
  const [pending, setPending] = useState<string | null>(null)
  const [addSelections, setAddSelections] = useState<Record<string, string>>({})

  const load = useCallback(async () => {
    if (!canView) return
    const token = await getFreshToken()
    setLoading(true)
    setError('')
    try {
      const [usersData, rolesData] = await Promise.all([listUsers(token), listRoles(token)])
      setUsers(usersData)
      setRoles(rolesData)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg.includes('401')) { logout(); navigate('/login', { replace: true }) }
      else setError('No se pudo cargar el listado de usuarios.')
    } finally {
      setLoading(false)
    }
  }, [canView, logout, navigate])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount es el patrón estándar de carga de datos
    load()
  }, [load])

  const visibleUsers = useMemo(() => {
    const needle = filter.trim().toLowerCase()
    const ordenados = [...users].sort((a, b) => a.username.localeCompare(b.username))
    if (!needle) return ordenados
    return ordenados.filter(u =>
      u.username.toLowerCase().includes(needle) ||
      (u.email ?? '').toLowerCase().includes(needle) ||
      u.effectiveRoles.some(r => r.toLowerCase().includes(needle)),
    )
  }, [users, filter])

  async function handleAssign(userId: string) {
    const role = addSelections[userId]
    if (!role) return
    const token = await getFreshToken()
    const key = `${userId}:${role}`
    setPending(key)
    setActionError('')
    try {
      await assignRole(token, userId, role)
      setAddSelections(prev => ({ ...prev, [userId]: '' }))
      await load()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'No se pudo asignar el rol.')
    } finally {
      setPending(null)
    }
  }

  async function handleRemove(userId: string, role: string) {
    const token = await getFreshToken()
    const key = `${userId}:${role}`
    setPending(key)
    setActionError('')
    try {
      await removeRole(token, userId, role)
      setConfirmRemove(null)
      await load()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'No se pudo quitar el rol.')
    } finally {
      setPending(null)
    }
  }

  function renderChip(u: UserWithRoles, role: string) {
    const key = `${u.id}:${role}`
    const isConfirming = confirmRemove?.userId === u.id && confirmRemove.role === role
    const isRemoving = pending === key

    // Heredado de un rol compuesto: no hay asignacion directa que borrar. Ofrecer la X
    // seria mentir, porque Keycloak devolveria 204 y el permiso seguiria en el token.
    const heredado = !u.realmRoles.includes(role)
    if (heredado) {
      return (
        <span
          key={role}
          title="Heredado de un rol. Para revocarlo hay que quitar el rol que lo incluye."
          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium opacity-60 ring-1 ring-inset ring-slate-300 ${roleChipClasses(role)}`}
        >
          {role}
          <span aria-hidden className="text-[10px]">↗</span>
        </span>
      )
    }

    if (isConfirming) {
      return (
        <span key={role} className="inline-flex items-center gap-1.5 rounded-full bg-red-50 border border-red-100 pl-2.5 pr-1.5 py-1 text-xs">
          <span className="text-red-600">¿Quitar {role}?</span>
          <button
            onClick={() => handleRemove(u.id, role)}
            disabled={isRemoving}
            className="font-medium text-red-600 hover:text-red-800 disabled:opacity-50 cursor-pointer"
          >
            {isRemoving ? '…' : 'Sí'}
          </button>
          <button
            onClick={() => setConfirmRemove(null)}
            className="text-slate-400 hover:text-slate-600 cursor-pointer"
          >
            No
          </button>
        </span>
      )
    }

    return (
      <span
        key={role}
        className={`inline-flex items-center gap-1 rounded-full pl-2.5 ${canEdit ? 'pr-1.5' : 'pr-2.5'} py-1 text-xs font-medium ${roleChipClasses(role)}`}
      >
        {role}
        {canEdit && (
          <button
            onClick={() => setConfirmRemove({ userId: u.id, role })}
            aria-label={`Quitar rol ${role}`}
            className="rounded-full p-0.5 hover:bg-black/10 transition-colors cursor-pointer"
          >
            <IconX />
          </button>
        )}
      </span>
    )
  }

  if (!canView) {
    return (
      <Layout>
        <div className="px-8 py-8 max-w-6xl mx-auto">
          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm px-6 py-16 flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
              <IconLock />
            </div>
            <h1 className="text-lg font-semibold text-slate-900">No tienes permiso para ver esta sección</h1>
            <p className="text-sm text-slate-400 max-w-sm">Se requiere el permiso user:manage para acceder a la administración de usuarios.</p>
          </div>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="px-8 py-8 max-w-5xl mx-auto">

        {/* Encabezado */}
        <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Usuarios y roles</h1>
            {!loading && (
              <p className="text-sm text-slate-400 mt-0.5">
                {users.length} usuarios · {roles.length} roles disponibles en el realm
              </p>
            )}
          </div>
          <input
            value={filter}
            onChange={e => setFilter(e.target.value)}
            placeholder="Filtrar por usuario, email o rol…"
            aria-label="Filtrar usuarios"
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition w-72"
          />
        </div>

        {/* Banner de error de acción (asignar/quitar), dismissible */}
        {actionError && (
          <div className="mb-5 flex items-center justify-between gap-3 rounded-xl bg-red-50 border border-red-100 px-4 py-3">
            <p className="text-sm text-red-600">{actionError}</p>
            <button
              onClick={() => setActionError('')}
              aria-label="Cerrar aviso"
              className="rounded-full p-1 text-red-400 hover:bg-red-100 hover:text-red-600 transition-colors cursor-pointer shrink-0"
            >
              <IconX className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {error && (
          <div className="mb-5 rounded-xl bg-red-50 border border-red-100 px-4 py-3">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        {loading && (
          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm px-6 py-16 text-center text-sm text-slate-300">
            Cargando…
          </div>
        )}

        {!loading && !error && visibleUsers.length === 0 && (
          <div className="rounded-2xl border border-slate-200 bg-white shadow-sm px-6 py-16 text-center text-sm text-slate-300">
            {users.length === 0 ? 'No se encontraron usuarios.' : 'Ningún usuario coincide con el filtro.'}
          </div>
        )}

        {/* Una tarjeta por usuario: la lista de roles es de largo variable y en una tabla
            desalineaba todas las columnas. */}
        <div className="space-y-4">
          {!loading && visibleUsers.map(u => {
            const isSelf = user?.username === u.username
            // Se dibuja lo efectivo, no lo directo: es lo que el usuario puede hacer.
            const { business, permissions } = splitRoles(u.effectiveRoles)
            // Se ofrece lo que el usuario no tiene ni por herencia: asignar algo que ya
            // recibe de un compuesto no cambia nada y solo ensucia la lista.
            const available = roles
              .filter(r => !u.effectiveRoles.includes(r.name))
              .sort((a, b) => a.name.localeCompare(b.name))
            const availableBusiness = available.filter(r => isBusinessRole(r.name))
            const availablePermissions = available.filter(r => !isBusinessRole(r.name))
            const selected = addSelections[u.id] ?? ''
            const addPending = pending === `${u.id}:${selected}` && selected !== ''

            return (
              <section
                key={u.id}
                data-testid="user-card"
                className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden"
              >

                {/* Identidad y estado */}
                <header className="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-4 border-b border-slate-100 bg-slate-50/60">
                  <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-700 text-sm font-bold shrink-0">
                    {u.username?.[0]?.toUpperCase()}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h2 className="font-semibold text-slate-900 truncate">{u.username}</h2>
                      {isSelf && (
                        <span
                          title="Estás editando tus propios roles"
                          className="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-700 shrink-0"
                        >
                          Tú
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-400 truncate">{u.email ?? 'Sin email'}</p>
                  </div>
                  <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium shrink-0 ${
                    u.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-400'
                  }`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${u.enabled ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                    {u.enabled ? 'Habilitado' : 'Deshabilitado'}
                  </span>
                </header>

                {/* Roles de negocio y permisos, en bloques separados */}
                <div className="px-5 py-4 space-y-4">
                  <div className="space-y-2">
                    <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">
                      Roles ({business.length})
                    </p>
                    <div className="flex flex-wrap items-center gap-1.5">
                      {business.length > 0
                        ? business.map(role => renderChip(u, role))
                        : <span className="text-xs text-slate-300">Sin roles asignados</span>}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wide">
                      Permisos ({permissions.length})
                    </p>
                    <div className="flex flex-wrap items-center gap-1.5">
                      {permissions.length > 0
                        ? permissions.map(role => renderChip(u, role))
                        : <span className="text-xs text-slate-300">Sin permisos directos</span>}
                    </div>
                  </div>
                </div>

                {/* Alta de rol, fuera del flujo de chips para que no se pierda entre ellos */}
                {canEdit && available.length > 0 && (
                  <footer className="flex flex-wrap items-center justify-end gap-2 px-5 py-3 border-t border-slate-100 bg-slate-50/60">
                    <select
                      value={selected}
                      onChange={e => setAddSelections(prev => ({ ...prev, [u.id]: e.target.value }))}
                      aria-label={`Agregar rol a ${u.username}`}
                      className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-600 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition cursor-pointer"
                    >
                      <option value="">+ Agregar rol</option>
                      {availableBusiness.length > 0 && (
                        <optgroup label="Roles">
                          {availableBusiness.map(r => (
                            <option key={r.name} value={r.name}>{r.name}</option>
                          ))}
                        </optgroup>
                      )}
                      {availablePermissions.length > 0 && (
                        <optgroup label="Permisos">
                          {availablePermissions.map(r => (
                            <option key={r.name} value={r.name}>{r.name}</option>
                          ))}
                        </optgroup>
                      )}
                    </select>
                    <button
                      onClick={() => handleAssign(u.id)}
                      disabled={!selected || addPending}
                      className="rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                    >
                      {addPending ? '…' : 'Agregar'}
                    </button>
                  </footer>
                )}
              </section>
            )
          })}
        </div>
      </div>
    </Layout>
  )
}
