<template>
  <canvas ref="canvasRef" class="particle-field" aria-hidden="true"></canvas>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  color: string
  alpha: number
  phase: number
  rotation: number
  rotationSpeed: number
  shape: 'dot' | 'rect' | 'star'
}

const props = withDefaults(
  defineProps<{
    /** 粒子密度倍率，移动端会自动减量 */
    density?: number
  }>(),
  {
    density: 1,
  },
)

const { theme, skin } = useTheme()
const canvasRef = ref<HTMLCanvasElement>()

const TAU = Math.PI * 2
const FRAME_INTERVAL = 1000 / 32

let ctx: CanvasRenderingContext2D | null = null
let particles: Particle[] = []
let width = 0
let height = 0
let dpr = 1
let raf = 0
let lastFrame = 0
let visible = true
let reducedMotion = false
let resizeObserver: ResizeObserver | null = null
let intersectionObserver: IntersectionObserver | null = null
let motionQuery: MediaQueryList | null = null

const fallbackPalette = ['#0066cc', '#0ec4b8', '#ff7759', '#f2b705']

const readPalette = (): { colors: string[]; opacity: number } => {
  const styles = getComputedStyle(document.documentElement)
  const colors = [1, 2, 3, 4]
    .map((index) => styles.getPropertyValue(`--home-particle-${index}`).trim())
    .filter(Boolean)
  const opacity = Number.parseFloat(styles.getPropertyValue('--home-particle-opacity')) || 0.55
  return {
    colors: colors.length > 0 ? colors : fallbackPalette,
    opacity: Math.min(Math.max(opacity, 0.15), 0.9),
  }
}

const createParticle = (colors: string[], opacity: number, isDark: boolean): Particle => {
  const shape: Particle['shape'] = isDark ? 'star' : Math.random() < 0.55 ? 'dot' : 'rect'

  return {
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.16,
    vy: (Math.random() - 0.5) * 0.2,
    size: isDark ? 0.8 + Math.random() * 2 : 1.5 + Math.random() * 3.5,
    color: colors[Math.floor(Math.random() * colors.length)],
    alpha: opacity * (0.35 + Math.random() * 0.65),
    phase: Math.random() * TAU,
    rotation: Math.random() * TAU,
    rotationSpeed: (Math.random() - 0.5) * 0.02,
    shape,
  }
}

const draw = (now: number) => {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)

  const time = now * 0.00035

  for (const particle of particles) {
    particle.x += particle.vx
    particle.y += particle.vy

    if (particle.x < -24) particle.x = width + 24
    if (particle.x > width + 24) particle.x = -24
    if (particle.y < -24) particle.y = height + 24
    if (particle.y > height + 24) particle.y = -24

    if (particle.shape === 'rect') {
      particle.rotation += particle.rotationSpeed
    }

    const twinkle = 0.62 + 0.38 * Math.sin(time + particle.phase)
    const alpha = particle.alpha * twinkle

    ctx.fillStyle = particle.color

    if (particle.shape === 'star') {
      // 先画柔和光晕，再画实心核心，避免使用昂贵的 shadowBlur
      ctx.globalAlpha = alpha * 0.2
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size * 3.2, 0, TAU)
      ctx.fill()

      ctx.globalAlpha = alpha
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size, 0, TAU)
      ctx.fill()
    } else if (particle.shape === 'rect') {
      ctx.globalAlpha = alpha
      ctx.save()
      ctx.translate(particle.x, particle.y)
      ctx.rotate(particle.rotation)
      ctx.fillRect(-particle.size, -particle.size * 0.5, particle.size * 2, particle.size)
      ctx.restore()
    } else {
      ctx.globalAlpha = alpha
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size * 0.8, 0, TAU)
      ctx.fill()
    }
  }

  ctx.globalAlpha = 1
}

const loop = (now: number) => {
  raf = requestAnimationFrame(loop)
  if (!visible || document.hidden) return
  if (now - lastFrame < FRAME_INTERVAL) return
  lastFrame = now
  draw(now)
}

const rebuild = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  width = Math.max(rect.width, 0)
  height = Math.max(rect.height, 0)
  if (width === 0 || height === 0) return

  dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)

  ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const { colors, opacity } = readPalette()
  const isDark = theme.value === 'dark'
  const isMobile = width < 768
  const density = isMobile ? 0.55 : props.density
  const count = Math.round(Math.min(90, Math.max(22, (width * height) / 26000)) * density)

  particles = Array.from({ length: count }, () => createParticle(colors, opacity, isDark))

  if (reducedMotion) {
    draw(performance.now())
  }
}

const stop = () => {
  if (raf) {
    cancelAnimationFrame(raf)
    raf = 0
  }
}

const start = () => {
  if (reducedMotion) {
    draw(performance.now())
    return
  }
  stop()
  lastFrame = 0
  raf = requestAnimationFrame(loop)
}

const onMotionChange = (event: MediaQueryListEvent) => {
  reducedMotion = event.matches
  if (reducedMotion) {
    stop()
    draw(performance.now())
  } else {
    start()
  }
}

onMounted(() => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion = motionQuery.matches
  motionQuery.addEventListener('change', onMotionChange)

  rebuild()
  start()

  if (canvasRef.value) {
    resizeObserver = new ResizeObserver(() => {
      rebuild()
      if (reducedMotion) draw(performance.now())
    })
    resizeObserver.observe(canvasRef.value)

    intersectionObserver = new IntersectionObserver(
      (entries) => {
        visible = entries.some((entry) => entry.isIntersecting)
      },
      { threshold: 0 },
    )
    intersectionObserver.observe(canvasRef.value)
  }
})

watch([theme, skin], () => {
  rebuild()
  if (reducedMotion) draw(performance.now())
})

onBeforeUnmount(() => {
  stop()
  resizeObserver?.disconnect()
  intersectionObserver?.disconnect()
  motionQuery?.removeEventListener('change', onMotionChange)
  resizeObserver = null
  intersectionObserver = null
  particles = []
  ctx = null
})
</script>

<style scoped>
.particle-field {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
