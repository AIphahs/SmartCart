import { NavLink } from 'react-router-dom'

const links = [
  { to: '/dashboard', icon: '📊', label: 'Tableau de bord' },
  { to: '/receipts', icon: '🧾', label: 'Reçus' },
  { to: '/compare', icon: '💰', label: 'Comparaison prix' },
  { to: '/stores', icon: '🏪', label: 'Magasins' },
]

export default function Sidebar() {
  return (
    <aside className="w-60 bg-white shadow-md flex flex-col shrink-0">
      <div className="p-6 border-b">
        <h1 className="text-xl font-bold text-emerald-600">🛒 SmartCart</h1>
        <p className="text-xs text-gray-400 mt-1">Analyse de courses</p>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {links.map(l => (
          <NavLink
            key={l.to}
            to={l.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-emerald-50 text-emerald-700'
                  : 'text-gray-600 hover:bg-gray-100'
              }`
            }
          >
            <span>{l.icon}</span>
            <span>{l.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
