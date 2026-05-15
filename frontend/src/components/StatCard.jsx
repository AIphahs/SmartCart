const colorMap = {
  emerald: 'bg-emerald-50 text-emerald-700',
  blue: 'bg-blue-50 text-blue-700',
  amber: 'bg-amber-50 text-amber-700',
  purple: 'bg-purple-50 text-purple-700',
}

export default function StatCard({ label, value, icon, color = 'emerald' }) {
  return (
    <div className="bg-white rounded-xl shadow p-5 flex items-center gap-4">
      <div className={`text-2xl w-12 h-12 flex items-center justify-center rounded-lg shrink-0 ${colorMap[color]}`}>
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-sm text-gray-500 truncate">{label}</p>
        <p className="text-xl font-bold text-gray-800 truncate">{value}</p>
      </div>
    </div>
  )
}
