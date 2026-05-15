import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api } from '../api/client'
import Spinner from '../components/Spinner'

const VALIDATION = {
  OK:       { label: 'Total vérifié',          cls: 'bg-emerald-100 text-emerald-700' },
  MISMATCH: { label: 'Écart détecté',          cls: 'bg-red-100 text-red-700' },
  NO_TOTAL: { label: 'Total non détecté',      cls: 'bg-yellow-100 text-yellow-700' },
  NO_ITEMS: { label: 'Aucun article extrait',  cls: 'bg-gray-100 text-gray-500' },
}

const CATEGORY_COLORS = {
  'Produits laitiers':       'bg-blue-100 text-blue-700',
  'Boulangerie & Pâtisserie':'bg-yellow-100 text-yellow-700',
  'Viandes & Poissons':      'bg-red-100 text-red-700',
  'Fruits & Légumes':        'bg-green-100 text-green-700',
  'Boissons':                'bg-cyan-100 text-cyan-700',
  'Surgelés':                'bg-indigo-100 text-indigo-700',
  'Épicerie':                'bg-orange-100 text-orange-700',
  'Hygiène & Beauté':        'bg-pink-100 text-pink-700',
  'Entretien':               'bg-gray-100 text-gray-600',
  'Autres':                  'bg-gray-100 text-gray-500',
}

export default function ReceiptDetail() {
  const { id } = useParams()
  const [receipt, setReceipt] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showRaw, setShowRaw] = useState(false)

  useEffect(() => {
    api.get(`/receipts/${id}`)
      .then(res => setReceipt(res.data))
      .catch(() => setError('Reçu introuvable.'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Spinner />
  if (error) return <p className="text-red-500 p-4">{error}</p>

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-4">
        <Link to="/receipts" className="text-gray-400 hover:text-gray-600 text-sm">← Retour</Link>
        <h1 className="text-2xl font-bold text-gray-800">Reçu #{receipt.id}</h1>
      </div>

      <div className="bg-white rounded-xl shadow p-6 grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div>
          <p className="text-xs text-gray-400 uppercase tracking-wide">Magasin</p>
          <p className="font-semibold text-gray-800 mt-0.5">{receipt.store?.name ?? '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400 uppercase tracking-wide">Date</p>
          <p className="font-semibold text-gray-800 mt-0.5">{receipt.purchaseDate ?? '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400 uppercase tracking-wide">Total</p>
          <p className="font-bold text-emerald-600 text-lg mt-0.5">
            {receipt.totalAmount != null ? `${Number(receipt.totalAmount).toFixed(2)} €` : '—'}
          </p>
        </div>
        <div>
          <p className="text-xs text-gray-400 uppercase tracking-wide">Articles</p>
          <p className="font-semibold text-gray-800 mt-0.5">{receipt.items?.length ?? 0}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400 uppercase tracking-wide">Somme articles</p>
          <p className="font-semibold text-gray-800 mt-0.5">
            {receipt.itemsTotal != null ? `${Number(receipt.itemsTotal).toFixed(2)} €` : '—'}
          </p>
        </div>
      </div>

      {receipt.validationStatus && (() => {
        const v = VALIDATION[receipt.validationStatus] ?? { label: receipt.validationStatus, cls: 'bg-gray-100 text-gray-500' }
        return (
          <div className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium ${v.cls}`}>
            <span>
              {receipt.validationStatus === 'OK' && '✅'}
              {receipt.validationStatus === 'MISMATCH' && '⚠️'}
              {receipt.validationStatus === 'NO_TOTAL' && '❓'}
              {receipt.validationStatus === 'NO_ITEMS' && '📭'}
            </span>
            <span>{v.label}</span>
            {receipt.validationStatus === 'MISMATCH' && receipt.totalDifference != null && (
              <span className="ml-auto font-normal">
                Différence : {Number(receipt.totalDifference) > 0 ? '+' : ''}{Number(receipt.totalDifference).toFixed(2)} €
                &nbsp;(total reçu {Number(receipt.totalAmount).toFixed(2)} € — somme articles {Number(receipt.itemsTotal).toFixed(2)} €)
              </span>
            )}
          </div>
        )
      })()}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="px-6 py-4 border-b">
          <h2 className="font-semibold text-gray-700">Articles détectés</h2>
        </div>
        {!receipt.items?.length ? (
          <p className="text-gray-400 text-center py-10 text-sm">Aucun article extrait</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
              <tr>
                <th className="px-6 py-3 text-left">Produit</th>
                <th className="px-6 py-3 text-left">Catégorie</th>
                <th className="px-6 py-3 text-right">Qté</th>
                <th className="px-6 py-3 text-right">Prix</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {receipt.items.map(item => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-6 py-3 text-gray-800">{item.productName}</td>
                  <td className="px-6 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${CATEGORY_COLORS[item.category] ?? 'bg-gray-100 text-gray-500'}`}>
                      {item.category ?? '—'}
                    </span>
                  </td>
                  <td className="px-6 py-3 text-right text-gray-500">{item.quantity ?? 1}</td>
                  <td className="px-6 py-3 text-right font-medium text-gray-800">
                    {item.totalPrice != null ? `${Number(item.totalPrice).toFixed(2)} €` : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-gray-50 border-t">
              <tr>
                <td colSpan={3} className="px-6 py-3 text-right font-semibold text-gray-700">Total</td>
                <td className="px-6 py-3 text-right font-bold text-emerald-600">
                  {receipt.totalAmount != null ? `${Number(receipt.totalAmount).toFixed(2)} €` : '—'}
                </td>
              </tr>
            </tfoot>
          </table>
        )}
      </div>
      <div className="bg-white rounded-xl shadow overflow-hidden">
        <button
          onClick={() => setShowRaw(v => !v)}
          className="w-full flex items-center justify-between px-6 py-4 text-left hover:bg-gray-50 transition-colors"
        >
          <div className="flex items-center gap-2">
            <span className="font-semibold text-gray-700">Texte brut Tesseract</span>
            <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">debug</span>
          </div>
          <span className="text-gray-400 text-sm">{showRaw ? '▲ Réduire' : '▼ Afficher'}</span>
        </button>

        {showRaw && (
          <div className="border-t px-6 py-4">
            {receipt.rawText ? (
              <pre className="text-xs text-gray-600 bg-gray-50 rounded-lg p-4 overflow-auto max-h-80 whitespace-pre-wrap font-mono leading-relaxed">
                {receipt.rawText}
              </pre>
            ) : (
              <p className="text-gray-400 text-sm text-center py-4">Aucun texte OCR disponible</p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
