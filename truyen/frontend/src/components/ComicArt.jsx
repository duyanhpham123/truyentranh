import { useId, useState } from 'react'

// Dữ liệu mẫu dùng ảnh placeholder xám. Với các ảnh đó, giao diện vẽ bìa và trang minh họa bằng SVG theo tên truyện.
const PLACEHOLDER = /placehold\.co/

const PALETTES = [
  { bg: '#FF3B30', mid: '#FFD60A', ink: '#14110F', paper: '#FFF7E6' },
  { bg: '#1D4ED8', mid: '#22D3EE', ink: '#0B1026', paper: '#F0F9FF' },
  { bg: '#7C3AED', mid: '#F472B6', ink: '#180B2E', paper: '#FDF4FF' },
  { bg: '#059669', mid: '#FDE047', ink: '#052E22', paper: '#F7FEE7' },
  { bg: '#F97316', mid: '#FEF08A', ink: '#2A1204', paper: '#FFFBEB' },
  { bg: '#DB2777', mid: '#FDE68A', ink: '#2E0618', paper: '#FFF1F2' },
  { bg: '#0F172A', mid: '#F43F5E', ink: '#020617', paper: '#F1F5F9' },
  { bg: '#0D9488', mid: '#FB7185', ink: '#042F2E', paper: '#F0FDFA' },
]

function hash(text) {
  let value = 2166136261
  for (const char of String(text)) {
    value ^= char.codePointAt(0)
    value = Math.imul(value, 16777619)
  }
  return value >>> 0
}

function pick(seed, shift, max) {
  return ((seed >>> shift) ^ (seed >>> (shift + 7))) % max
}

function speedLines(cx, cy, radius, count, seed) {
  const lines = []
  for (let i = 0; i < count; i++) {
    const angle = (i / count) * Math.PI * 2 + pick(seed, i % 16, 10) / 100
    const spread = 0.022 + pick(seed, (i + 5) % 16, 4) / 200
    lines.push(
      `M${cx} ${cy}L${cx + Math.cos(angle - spread) * radius} ${cy + Math.sin(angle - spread) * radius}` +
        `L${cx + Math.cos(angle + spread) * radius} ${cy + Math.sin(angle + spread) * radius}Z`,
    )
  }
  return lines.join('')
}

function wrapTitle(title, maxChars) {
  const lines = []
  let line = ''
  for (const word of title.split(/\s+/)) {
    if (line && (line + ' ' + word).length > maxChars) {
      lines.push(line)
      line = word
    } else {
      line = line ? `${line} ${word}` : word
    }
  }
  if (line) lines.push(line)
  return lines.slice(0, 4)
}

const SILHOUETTES = [
  // Núi
  'M0 320L60 210L100 262L165 150L232 268L268 222L300 288V440H0Z',
  // Thành phố
  'M0 300H34V232H74V272H104V180H150V250H182V206H226V268H262V226H300V440H0Z',
  // Sóng
  'M0 300C40 250 70 340 112 296S184 240 222 292S272 330 300 280V440H0Z',
  // Nhân vật mặc áo choàng
  'M150 142C176 142 192 162 192 186C192 204 184 216 176 224L232 262L262 440H38L68 262L124 224C116 216 108 204 108 186C108 162 124 142 150 142Z',
]

/** Bìa minh họa vẽ bằng SVG, mỗi tên truyện cho ra một bìa cố định. */
export function ComicArt({ title }) {
  const id = useId()
  const seed = hash(title)
  const palette = PALETTES[seed % PALETTES.length]
  const silhouette = SILHOUETTES[pick(seed, 8, SILHOUETTES.length)]
  const cx = 90 + pick(seed, 3, 120)
  const cy = 90 + pick(seed, 11, 70)
  const lines = wrapTitle(title.toLocaleUpperCase('vi-VN'), 12)
  const longest = Math.max(...lines.map((line) => line.length))
  // Chữ Dela Gothic rộng khoảng 0.8em, tính cỡ để dòng dài nhất nằm gọn trong bìa
  const fontSize = Math.min(34, Math.floor(258 / (longest * 0.8)))
  const lineHeight = fontSize * 1.14
  const bandHeight = lines.length * lineHeight + 26

  return (
    <svg className="comic-art" viewBox="0 0 300 440" role="img" aria-label={`Bìa truyện ${title}`} preserveAspectRatio="xMidYMid slice">
      <defs>
        <pattern id={`${id}-dots`} width="9" height="9" patternUnits="userSpaceOnUse" patternTransform="rotate(30)">
          <circle cx="4.5" cy="4.5" r="2.3" fill={palette.ink} opacity="0.24" />
        </pattern>
      </defs>
      <rect width="300" height="440" fill={palette.bg} />
      <path d={speedLines(cx, cy, 620, 34, seed)} fill={palette.mid} opacity="0.5" />
      <circle cx={cx} cy={cy} r="68" fill={palette.paper} stroke={palette.ink} strokeWidth="5" />
      <circle cx={cx} cy={cy} r="68" fill={`url(#${id}-dots)`} />
      <path d={silhouette} fill={palette.ink} />
      <rect width="300" height="440" fill={`url(#${id}-dots)`} opacity="0.45" />
      <g transform={`translate(0 ${440 - bandHeight - 12}) rotate(-2.2 150 0)`}>
        <rect x="-12" y="0" width="324" height={bandHeight + 34} fill={palette.mid} stroke={palette.ink} strokeWidth="5" />
        <text fontFamily="'Dela Gothic One', sans-serif" fontSize={fontSize} fill={palette.ink} textAnchor="middle">
          {lines.map((line, index) => (
            <tspan key={index} x="150" y={13 + fontSize + index * lineHeight}>
              {line}
            </tspan>
          ))}
        </text>
      </g>
    </svg>
  )
}

/** Ảnh bìa thật; không có ảnh, ảnh placeholder hoặc ảnh lỗi thì dùng bìa minh họa. */
export function Cover({ url, title, className = '' }) {
  const [failed, setFailed] = useState(false)
  const useArt = !url || failed || PLACEHOLDER.test(url)
  return (
    <div className={`cover ${className}`}>
      {useArt ? (
        <ComicArt title={title} />
      ) : (
        <img src={url} alt={`Bìa truyện ${title}`} loading="lazy" onError={() => setFailed(true)} />
      )}
    </div>
  )
}

const LAYOUTS = [
  [[20, 20, 760, 400], [20, 440, 370, 360], [410, 440, 370, 360], [20, 820, 760, 360]],
  [[20, 20, 370, 540], [410, 20, 370, 260], [410, 300, 370, 260], [20, 580, 760, 600]],
  [[20, 20, 760, 300], [20, 340, 240, 440], [280, 340, 500, 440], [20, 800, 500, 380], [540, 800, 240, 380]],
]

const BUBBLES = ['BÙM!!', 'Cái gì?!', 'Chạy mau!', 'Khoan đã...', 'Hự!', 'Không thể nào!', 'Xoẹt!', 'Đi thôi!', 'Hừm...']

/** Trang truyện minh họa cho dữ liệu mẫu. */
function PageArt({ seedText, number }) {
  const seed = hash(seedText)
  const palette = PALETTES[hash(seedText.split('#')[0]) % PALETTES.length]
  const panels = LAYOUTS[seed % LAYOUTS.length]
  return (
    <svg className="page-art" viewBox="0 0 800 1200" role="img" aria-label={`Trang minh họa số ${number}`}>
      <rect width="800" height="1200" fill="#FBF7EC" />
      {panels.map(([x, y, width, height], index) => {
        const panelSeed = hash(`${seedText}-${index}`)
        const cx = x + width * (0.3 + pick(panelSeed, 2, 40) / 100)
        const cy = y + height * (0.34 + pick(panelSeed, 6, 30) / 100)
        const clip = `${seedText.replace(/\W/g, '')}-${index}`
        const figure = 28 + pick(panelSeed, 9, 34)
        const bubble = BUBBLES[pick(panelSeed, 4, BUBBLES.length)]
        const bx = x + 18 + pick(panelSeed, 12, Math.max(1, width - 270))
        return (
          <g key={index}>
            <clipPath id={clip}>
              <rect x={x} y={y} width={width} height={height} />
            </clipPath>
            <g clipPath={`url(#${clip})`}>
              <rect x={x} y={y} width={width} height={height} fill={index % 2 ? palette.mid : palette.paper} />
              <path d={speedLines(cx, cy, 1000, 40, panelSeed)} fill={index % 2 ? palette.bg : palette.ink} opacity={index % 2 ? 0.45 : 0.16} />
              <circle cx={cx} cy={cy} r={figure} fill={palette.ink} />
              <path d={`M${cx - figure * 1.2} ${y + height}L${cx - figure * 0.8} ${cy + figure}H${cx + figure * 0.8}L${cx + figure * 1.2} ${y + height}Z`} fill={palette.ink} />
            </g>
            <rect x={x} y={y} width={width} height={height} fill="none" stroke="#14110F" strokeWidth="7" />
            <ellipse cx={bx + 116} cy={y + 62} rx="114" ry="38" fill="#fff" stroke="#14110F" strokeWidth="4" />
            <text x={bx + 116} y={y + 70} textAnchor="middle" fontFamily="'Be Vietnam Pro', sans-serif" fontWeight="800" fontSize="22" fill="#14110F">
              {bubble}
            </text>
          </g>
        )
      })}
      <text x="780" y="1194" textAnchor="end" fontFamily="'Be Vietnam Pro', sans-serif" fontSize="16" fill="#6B6459">
        Ảnh minh họa · trang {number}
      </text>
    </svg>
  )
}

/** Trang truyện thật có width/height thì giữ chỗ sẵn, tránh giật layout khi ảnh tải xong. */
export function ChapterImage({ page, seedText }) {
  const [failed, setFailed] = useState(false)
  if (failed || PLACEHOLDER.test(page.imageUrl)) {
    return <PageArt seedText={`${seedText}#${page.pageNumber}`} number={page.pageNumber} />
  }
  return (
    <img
      src={page.imageUrl}
      alt={`Trang ${page.pageNumber}`}
      loading="lazy"
      width={page.width ?? undefined}
      height={page.height ?? undefined}
      onError={() => setFailed(true)}
    />
  )
}
