import { useState } from 'react'
import { api } from '../api/client'
import { DAYS, formatTime } from '../utils/days'

function initHours(store) {
  return DAYS.map(d => {
    const existing = store.hours?.find(h => h.dayOfWeek === d.value)
    return {
      dayOfWeek: d.value,
      closed: existing ? existing.closed : true,
      openTime: existing ? formatTime(existing.openTime) : '09:00',
      closeTime: existing ? formatTime(existing.closeTime) : '19:00',
    }
  })
}

export default function StoreHoursModal({ store, onClose, onSuccess }) {
  const [address, setAddress] = useState(store.address || '')
  const [phone, setPhone] = useState(store.phone || '')
  const [website, setWebsite] = useState(store.website || '')
  const [hours, setHours] = useState(initHours(store))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const updateDay = (index, patch) => {
    setHours(hs => hs.map((h, i) => (i === index ? { ...h, ...patch } : h)))
  }

  const handleSubmit = async e => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const payload = {
        address: address.trim() || null,
        phone: phone.trim() || null,
        website: website.trim() || null,
        hours: hours.map(h => ({
          dayOfWeek: h.dayOfWeek,
          closed: h.closed,
          openTime: h.closed ? null : h.openTime,
          closeTime: h.closed ? null : h.closeTime,
        })),
      }
      const res = await api.put(`/stores/${store.id}`, payload)
      onSuccess(res.data)
    } catch (err) {
      setError(err.response?.data?.detail || "Erreur lors de l'enregistrement.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold text-gray-800">Modifier {store.name}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <div className="grid grid-cols-1 gap-3">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Adresse</label>
              <input
                type="text"
                value={address}
                onChange={e => setAddress(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Téléphone</label>
                <input
                  type="tel"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  placeholder="01 23 45 67 89"
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Site web</label>
                <input
                  type="text"
                  value={website}
                  onChange={e => setWebsite(e.target.value)}
                  placeholder="exemple.fr"
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            </div>
          </div>

          <div>
            <p className="text-sm text-gray-600 mb-2">Horaires d'ouverture</p>
            <div className="space-y-2">
              {DAYS.map((d, i) => {
                const h = hours[i]
                return (
                  <div key={d.value} className="flex items-center gap-3">
                    <span className="w-20 text-sm text-gray-600 shrink-0">{d.label}</span>
                    <label className="flex items-center gap-1.5 text-xs text-gray-500 shrink-0">
                      <input
                        type="checkbox"
                        checked={h.closed}
                        onChange={e => updateDay(i, { closed: e.target.checked })}
                      />
                      Fermé
                    </label>
                    {!h.closed && (
                      <>
                        <input
                          type="time"
                          value={h.openTime}
                          onChange={e => updateDay(i, { openTime: e.target.value })}
                          className="border rounded-lg px-2 py-1 text-sm flex-1 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                        />
                        <span className="text-gray-400 text-sm">–</span>
                        <input
                          type="time"
                          value={h.closeTime}
                          onChange={e => updateDay(i, { closeTime: e.target.value })}
                          className="border rounded-lg px-2 py-1 text-sm flex-1 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                        />
                      </>
                    )}
                  </div>
                )
              })}
            </div>
          </div>

          {error && <p className="text-red-500 text-sm">{error}</p>}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border rounded-lg text-sm text-gray-600 hover:bg-gray-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50"
            >
              {loading ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
