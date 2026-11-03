import { useMemo, useState } from 'react'
import { fullNumber } from '../../lib/format.js'

function niceMax(value) {
  if (value <= 0) return 1
  const power = 10 ** Math.floor(Math.log10(value))
  for (const step of [1, 2, 5, 10]) {
    if (value <= step * power) return step * power
  }
  return 10 * power
}

const shortDate = (iso) => {
  const [, month, day] = iso.split('-')
  return `${day}/${month}`
}

const W = 720
const H = 260
const PAD = { top: 16, right: 16, bottom: 30, left: 52 }

/** Biểu đồ đường một chuỗi theo ngày, rê chuột hiện giá trị từng ngày. */
export function LineChart({ data, unit }) {
  const [hover, setHover] = useState(null)
  const { points, max, ticks } = useMemo(() => {
    const top = niceMax(Math.max(...data.map((item) => item.count), 0))
    const stepX = data.length > 1 ? (W - PAD.left - PAD.right) / (data.length - 1) : 0
    return {
      max: top,
      ticks: [0, 0.25, 0.5, 0.75, 1].map((ratio) => Math.round(top * ratio)),
      points: data.map((item, index) => ({
        ...item,
        x: data.length > 1 ? PAD.left + index * stepX : (W + PAD.left - PAD.right) / 2,
        y: PAD.top + (1 - item.count / top) * (H - PAD.top - PAD.bottom),
      })),
    }
  }, [data])

  if (!data.length) return <p className="muted">Chưa có dữ liệu trong khoảng này.</p>

  const yOf = (value) => PAD.top + (1 - value / max) * (H - PAD.top - PAD.bottom)
  const line = points.map((point, index) => `${index ? 'L' : 'M'}${point.x} ${point.y}`).join('')
  const area = `${line}L${points[points.length - 1].x} ${H - PAD.bottom}L${points[0].x} ${H - PAD.bottom}Z`
  const every = Math.max(1, Math.ceil(points.length / 7))
  const active = hover == null ? null : points[hover]

  const onMove = (event) => {
    const rect = event.currentTarget.getBoundingClientRect()
    const x = ((event.clientX - rect.left) / rect.width) * W
    let nearest = 0
    points.forEach((point, index) => {
      if (Math.abs(point.x - x) < Math.abs(points[nearest].x - x)) nearest = index
    })
    setHover(nearest)
  }

  return (
    <div className="chart">
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Biểu đồ ${unit} theo ngày`} onMouseMove={onMove} onMouseLeave={() => setHover(null)}>
        {ticks.map((tick) => (
          <g key={tick}>
            <line x1={PAD.left} x2={W - PAD.right} y1={yOf(tick)} y2={yOf(tick)} className="chart-grid" />
            <text x={PAD.left - 8} y={yOf(tick) + 4} textAnchor="end" className="chart-label">
              {fullNumber(tick)}
            </text>
          </g>
        ))}
        {points.map(
          (point, index) =>
            index % every === 0 && (
              <text key={point.date} x={point.x} y={H - 8} textAnchor="middle" className="chart-label">
                {shortDate(point.date)}
              </text>
            ),
        )}
        <path d={area} className="chart-area" />
        <path d={line} className="chart-line" />
        {active && (
          <>
            <line x1={active.x} x2={active.x} y1={PAD.top} y2={H - PAD.bottom} className="chart-crosshair" />
            <circle cx={active.x} cy={active.y} r="5" className="chart-dot" />
          </>
        )}
      </svg>
      {active && (
        <div className="chart-tip" style={{ left: `${(active.x / W) * 100}%`, top: `${(active.y / H) * 100}%` }}>
          <strong>{fullNumber(active.count)}</strong> {unit}
          <small>{shortDate(active.date)}</small>
        </div>
      )}
      <details className="chart-table">
        <summary>Xem dạng bảng</summary>
        <table>
          <thead>
            <tr>
              <th>Ngày</th>
              <th>{unit}</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr key={item.date}>
                <td>{shortDate(item.date)}</td>
                <td>{fullNumber(item.count)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}

/** Xếp hạng theo lượt xem: mỗi hàng có sẵn nhãn và số, thanh chỉ để so sánh độ lớn. */
export function BarList({ items }) {
  if (!items.length) return <p className="muted">Chưa có dữ liệu trong khoảng này.</p>
  const max = Math.max(...items.map((item) => item.value), 1)
  return (
    <ul className="bar-list">
      {items.map((item) => (
        <li key={item.key} title={`${item.label}: ${fullNumber(item.value)} lượt xem`}>
          <span className="bar-label">{item.label}</span>
          <span className="bar-value">{fullNumber(item.value)}</span>
          <span className="bar-track">
            <span className="bar-fill" style={{ width: `${(item.value / max) * 100}%` }} />
          </span>
        </li>
      ))}
    </ul>
  )
}
