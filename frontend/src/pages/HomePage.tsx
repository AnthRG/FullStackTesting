import { useAuth } from '../auth/AuthContext'

export default function HomePage() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-4">
          <span className="font-semibold text-slate-900">Full Stack Testing</span>
          <button
            onClick={logout}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 transition hover:bg-slate-100"
          >
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-10">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="mb-1 text-lg font-semibold text-slate-900">Sesión iniciada</h1>
          <p className="mb-6 text-sm text-slate-500">
            Datos de{' '}
            <code className="rounded bg-slate-100 px-1.5 py-0.5 text-slate-700">/api/auth/me</code>{' '}
            (validado por Spring Security).
          </p>
          <dl className="space-y-3 text-sm">
            <div className="flex gap-4">
              <dt className="w-20 shrink-0 text-slate-500">Usuario</dt>
              <dd className="font-medium text-slate-900">{user?.username}</dd>
            </div>
            <div className="flex gap-4">
              <dt className="w-20 shrink-0 text-slate-500">Email</dt>
              <dd className="text-slate-900">{user?.email ?? '—'}</dd>
            </div>
            <div className="flex gap-4">
              <dt className="w-20 shrink-0 text-slate-500">Roles</dt>
              <dd className="flex flex-wrap gap-1.5">
                {user && user.roles.length > 0
                  ? user.roles.map((role) => (
                      <span
                        key={role}
                        className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700"
                      >
                        {role}
                      </span>
                    ))
                  : '—'}
              </dd>
            </div>
          </dl>
        </div>
      </main>
    </div>
  )
}
