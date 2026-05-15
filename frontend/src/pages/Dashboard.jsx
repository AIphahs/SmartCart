import { useState, useEffect } from 'react'
import { api } from '../api/client'
import StatCard from '../components/StatCard'
import Spinner from '../components/Spinner'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
} from 'recharts'

const COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316']

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [monthly, setMonthly] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      api.get('/analytics/spending/total'),
      api.get('/analytics/spending/monthly?months=12'),
      api.get('/analytics/spending/categories'),
    ])
      .then(([statsRes, monthlyRes, catsRes]) => {
        setStats(statsRes.data)
        setMonthly([...monthlyRes.data].reverse())
        setCategories(catsRes.data)
      })
      .catch(() => setError('Impossible de charger les données.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />
  if (error) return <p className="text-red-500">{error}</p>

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Tableau de bord</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total dépensé" value={`${(stats?.totalSpending ?? 0).toFixed(2)} €`} icon="💶" color="emerald" />
        <StatCard label="Reçus scannés" value={stats?.receiptCount ?? 0} icon="🧾" color="blue" />
        <StatCard label="Moyenne / reçu" value={`${(stats?.averagePerReceipt ?? 0).toFixed(2)} €`} icon="📊" color="amber" />
        <StatCard label="Premier achat" value={stats?.firstReceiptDate ?? '—'} icon="📅" color="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="text-base font-semibold text-gray-700 mb-4">Dépenses mensuelles (12 mois)</h2>
          {monthly.length === 0 ? (
            <p className="text-gray-400 text-center py-10 text-sm">Aucune donnée disponible</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={monthly} margin={{ bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="monthLabel" tick={{ fontSize: 10 }} angle={-30} textAnchor="end" interval={0} />
                <YAxis tickFormatter={v => `${v}€`} tick={{ fontSize: 11 }} />
                <Tooltip formatter={v => [`${Number(v).toFixed(2)} €`, 'Total']} />
                <Bar dataKey="total" fill="#10b981" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="text-base font-semibold text-gray-700 mb-4">Répartition par catégorie</h2>
          {categories.length === 0 ? (
            <p className="text-gray-400 text-center py-10 text-sm">Aucune donnée disponible</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={categories}
                  dataKey="total"
                  nameKey="category"
                  cx="50%"
                  cy="50%"
                  outerRadius={90}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {categories.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={v => [`${Number(v).toFixed(2)} €`]} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  )
}
