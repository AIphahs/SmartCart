import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api/client'
import Spinner from '../components/Spinner'
import StoreHoursModal from '../components/StoreHoursModal'
import { DAYS, formatTime } from '../utils/days'

export default function StoreDetail() {
  const { id } = useParams()
  const [store, setStore] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)

  const load = () => {
    api.get(`/stores/${id}`)
      .then(res => setStore(res.data))
      .catch(() => setError('Magasin introuvable.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [id])

  if (loading) return <Spinner />
  if (error) return <p className="text-red-500 p-4">{error}</p>

  const today = new Date().getDay() === 0 ? 7 : new Date().getDay()

  return (
    <div className="space-y-6 max-w-2xl">
      <div className="flex items-center gap-4">
        <Link to="/stores" className="text-gray-400 hover:text-gray-600 text-sm">← Retour</Link>
        <h1 className="text-2xl font-bold text-gray-800">{store.name}</h1>
      </div>

      <div className="bg-white rounded-xl shadow p-6 space-y-4">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            {store.address && <p className="text-sm text-gray-600">📍 {store.address}</p>}
            {store.phone && <p className="text-sm text-gray-600">☎️ {store.phone}</p>}
            {store.website && <p className="text-sm text-gray-600">🌐 {store.website}</p>}
            <p className="text-sm text-gray-500">{store.receiptCount} reçu{store.receiptCount !== 1 ? 's' : ''}</p>
          </div>
          <button
            onClick={() => setEditing(true)}
            className="text-emerald-600 text-sm font-medium hover:underline shrink-0"
          >
            ✎ Modifier
          </button>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="px-6 py-4 border-b flex items-center justify-between">
          <h2 className="font-semibold text-gray-700">🕐 Horaires d'ouverture</h2>
          {store.hours?.length > 0 && (
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
              store.openNow ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'
            }`}>
              {store.openNow ? '🟢 Ouvert maintenant' : '⚪ Fermé'}
            </span>
          )}
        </div>

        {!store.hours?.length ? (
          <p className="text-gray-400 text-center py-8 text-sm">Horaires non renseignés</p>
        ) : (
          <div className="divide-y divide-gray-100">
            {DAYS.map(d => {
              const h = store.hours.find(x => x.dayOfWeek === d.value)
              const isToday = d.value === today
              return (
                <div
                  key={d.value}
                  className={`flex items-center justify-between px-6 py-2.5 text-sm ${isToday ? 'bg-emerald-50/60' : ''}`}
                >
                  <span className={isToday ? 'font-semibold text-gray-800' : 'text-gray-600'}>{d.label}</span>
                  <span className={isToday ? 'font-semibold text-gray-800' : 'text-gray-500'}>
                    {!h || h.closed ? 'Fermé' : `${formatTime(h.openTime)} – ${formatTime(h.closeTime)}`}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {editing && (
        <StoreHoursModal
          store={store}
          onClose={() => setEditing(false)}
          onSuccess={updated => { setStore(updated); setEditing(false) }}
        />
      )}
    </div>
  )
}
