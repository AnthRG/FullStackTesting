import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { assignRole, listRoles, listUsers, removeRole } from '../adminApi'
import type { Role, UserWithRoles } from '../adminApi'
import Layout from '../components/Layout'

// El gating de esta página es solo UX: oculta controles según el token.
// La protección real está en el backend, donde UserRolesController exige user:manage
// en los cinco endpoints de /api/admin/** (ver AuthorizationIT).

const TOKEN_KEY = 'access_token'

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

// Un color por modulo, y los roles de negocio (los que no llevan ':') destacados aparte,
// para que se vea de un vistazo la diferencia entre un rol y los permisos que agrupa.
function roleChipClasses(role: string): string {
  if (!role.includes(':')) return 'bg-blue-100 text-blue-800 font-semibold'
  if (role.startsWith('product:')) return 'bg-violet-50 text-violet-700'
  if (role.startsWith('stock:')) return 'bg-emerald-50 text-emerald-700'
  if (role.startsWith('report:')) return 'bg-sky-50 text-sky-700'
  if (role.startsWith('audit:')) return 'bg-orange-50 text-orange-700'
  if (role.startsWith('user:')) return 'bg-rose-50 text-rose-700'
  return 'bg-amber-50 text-amber-700'
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

  const [confirmRemove, setConfirmRemove] = useState<{ userId: string; role: string } | null>(null)
  const [pending, setPending] = useState<string | null>(null)
  const [addSelections, setAddSelections] = useState<Record<string, string>>({})

  const load = useCallback(async () => {
    if (!canView) return
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
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

  async function handleAssign(userId: string) {
    const role = addSelections[userId]
    if (!role) return
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
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
    const token = localStorage.getItem(TOKEN_KEY) ?? ''
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
      <div className="px-8 py-8 max-w-6xl mx-auto">

        {/* Encabezado */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Usuarios y roles</h1>
            {!loading && (
              <p className="text-sm text-slate-400 mt-0.5">{users.length} usuarios en total</p>
            )}
          </div>
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

        {/* Tabla */}
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          {error && (
            <p className="px-6 py-4 text-sm text-red-500 border-b border-slate-100">{error}</p>
          )}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50">
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Usuario</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Email</th>
                  <th className="px-5 py-3 text-center text-xs font-semibold text-slate-400 uppercase tracking-wide">Estado</th>
                  <th className="px-5 py-3 text-left text-xs font-semibold text-slate-400 uppercase tracking-wide">Roles</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading && (
                  <tr>
                    <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-300">
                      Cargando…
                    </td>
                  </tr>
                )}
                {!loading && !error && users.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-300">
                      No se encontraron usuarios.
                    </td>
                  </tr>
                )}
                {!loading && users.map(u => {
                  const isSelf = user?.username === u.username
                  const available = roles.filter(r => !u.realmRoles.includes(r.name))
                  const selected = addSelections[u.id] ?? ''
                  const addKey = `${u.id}:${selected}`
                  const addPending = pending === addKey && selected !== ''

                  return (
                    <tr key={u.id} className="hover:bg-blue-50/50 transition-colors align-top">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-blue-100 flex items-center justify-center text-blue-700 text-sm font-bold shrink-0">
                            {u.username?.[0]?.toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-slate-900 truncate">{u.username}</p>
                            {canEdit && isSelf && (
                              <p className="text-xs text-amber-600 mt-0.5">Estás editando tus propios roles</p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4 text-slate-600">{u.email ?? '—'}</td>
                      <td className="px-5 py-4 text-center">
                        <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          u.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-400'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${u.enabled ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                          {u.enabled ? 'Habilitado' : 'Deshabilitado'}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap items-center gap-1.5">
                          {u.realmRoles.map(role => {
                            const key = `${u.id}:${role}`
                            const isConfirming = confirmRemove?.userId === u.id && confirmRemove.role === role
                            const isRemoving = pending === key

                            if (isConfirming) {
                              return (
                                <span key={role} className="inline-flex items-center gap-1.5 rounded-full bg-red-50 pl-2.5 pr-1.5 py-1 text-xs">
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
                          })}

                          {u.realmRoles.length === 0 && !canEdit && (
                            <span className="text-xs text-slate-300">Sin roles</span>
                          )}

                          {canEdit && available.length > 0 && (
                            <span className="inline-flex items-center gap-1">
                              <select
                                value={selected}
                                onChange={e => setAddSelections(prev => ({ ...prev, [u.id]: e.target.value }))}
                                aria-label={`Agregar rol a ${u.username}`}
                                className="rounded-full border border-slate-200 bg-white px-2 py-1 text-xs text-slate-600 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition cursor-pointer"
                              >
                                <option value="">+ Agregar rol</option>
                                {available.map(r => (
                                  <option key={r.name} value={r.name}>{r.name}</option>
                                ))}
                              </select>
                              <button
                                onClick={() => handleAssign(u.id)}
                                disabled={!selected || addPending}
                                className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                              >
                                {addPending ? '…' : 'Agregar'}
                              </button>
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Layout>
  )
}
