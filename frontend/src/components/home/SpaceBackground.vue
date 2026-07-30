<template>
  <div class="space-background" ref="containerRef">
    <canvas ref="canvasRef"></canvas>
    
    <div class="neural-lines">
      <svg class="lines-svg" preserveAspectRatio="none">
        <defs>
          <linearGradient id="lineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" style="stop-color:#7C5CFF;stop-opacity:0" />
            <stop offset="50%" style="stop-color:#7C5CFF;stop-opacity:0.3" />
            <stop offset="100%" style="stop-color:#00E5C0;stop-opacity:0" />
          </linearGradient>
        </defs>
      </svg>
    </div>

    <div class="glow orb orb-1"></div>
    <div class="glow orb orb-2"></div>
    <div class="glow orb orb-3"></div>
    <div class="glow orb orb-4"></div>

    <div class="grid-overlay"></div>
    <div class="vignette"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
const containerRef = ref(null)
let animationId = null
let particles = []
let mouseX = 0
let mouseY = 0
let time = 0

const PARTICLE_COUNT = 200
const CONNECTION_DISTANCE = 120

const initCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  const w = window.innerWidth
  const h = window.innerHeight

  canvas.width = w
  canvas.height = h

  particles = []
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      baseX: 0,
      baseY: 0,
      size: Math.random() * 2 + 0.5,
      speedX: (Math.random() - 0.5) * 0.3,
      speedY: (Math.random() - 0.5) * 0.3,
      opacity: Math.random() * 0.5 + 0.2,
      twinkleSpeed: Math.random() * 0.02 + 0.005,
      twinklePhase: Math.random() * Math.PI * 2,
      hue: Math.random() * 60 + 240
    })
    particles[i].baseX = particles[i].x
    particles[i].baseY = particles[i].y
  }
}

const drawConnections = (ctx) => {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < CONNECTION_DISTANCE) {
        const opacity = (1 - distance / CONNECTION_DISTANCE) * 0.2
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(124, 92, 255, ${opacity})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
}

const animate = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  
  time += 0.01

  ctx.clearRect(0, 0, w, h)

  particles.forEach(p => {
    p.x += p.speedX
    p.y += p.speedY
    p.twinklePhase += p.twinkleSpeed

    if (p.x < 0) p.x = w
    if (p.x > w) p.x = 0
    if (p.y < 0) p.y = h
    if (p.y > h) p.y = 0

    const dx = mouseX - p.x
    const dy = mouseY - p.y
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist > 0 && dist < 200) {
      const force = (200 - dist) / 200 * 0.5
      p.x += (dx / dist) * force
      p.y += (dy / dist) * force
    }

    if (!isFinite(p.x) || !isFinite(p.y)) {
      p.x = Math.random() * w
      p.y = Math.random() * h
      p.size = Math.random() * 2 + 0.5
    }

    const twinkle = Math.sin(p.twinklePhase) * 0.3 + 0.7
    const radius = Math.max(1, p.size * 3)
    const gradient = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, radius)
    gradient.addColorStop(0, `hsla(${p.hue}, 80%, 70%, ${p.opacity * twinkle})`)
    gradient.addColorStop(0.5, `hsla(${p.hue}, 80%, 60%, ${p.opacity * twinkle * 0.3})`)
    gradient.addColorStop(1, 'transparent')

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size * 3, 0, Math.PI * 2)
    ctx.fillStyle = gradient
    ctx.fill()

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${p.hue}, 90%, 80%, ${Math.min(1, p.opacity * twinkle * 1.5)})`
    ctx.fill()
  })

  drawConnections(ctx)

  animationId = requestAnimationFrame(animate)
}

const handleMouseMove = (e) => {
  mouseX = e.clientX
  mouseY = e.clientY
}

const handleResize = () => initCanvas()

onMounted(() => {
  initCanvas()
  animate()
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style lang="scss" scoped>
.space-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
  background: 
    radial-gradient(ellipse at 20% 20%, rgba(124, 92, 255, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(0, 229, 192, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, #0a0e27 0%, #060816 70%);

  canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }
}

.neural-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  opacity: 0.3;

  .lines-svg {
    width: 100%;
    height: 100%;
  }
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
  transition: transform 0.3s ease;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.4) 0%, transparent 70%);
  top: -150px;
  right: -150px;
  animation: floatOrb1 25s ease-in-out infinite;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
  bottom: -100px;
  left: 10%;
  animation: floatOrb2 30s ease-in-out infinite;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(0, 229, 192, 0.25) 0%, transparent 70%);
  top: 30%;
  left: -100px;
  animation: floatOrb3 35s ease-in-out infinite;
}

.orb-4 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(240, 147, 251, 0.2) 0%, transparent 70%);
  top: 60%;
  right: 20%;
  animation: floatOrb4 40s ease-in-out infinite;
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    linear-gradient(rgba(124, 92, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(124, 92, 255, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
}

.vignette {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at center, transparent 50%, rgba(6, 8, 22, 0.8) 100%);
  pointer-events: none;
}

@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(50px, -50px) scale(1.05); }
  50% { transform: translate(-30px, -80px) scale(1); }
  75% { transform: translate(-50px, 30px) scale(0.95); }
}

@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-40px, 30px) scale(0.95); }
  50% { transform: translate(20px, 60px) scale(1.05); }
  75% { transform: translate(40px, -20px) scale(1); }
}

@keyframes floatOrb3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(60px, 40px) scale(1.1); }
  66% { transform: translate(-40px, -60px) scale(0.9); }
}

@keyframes floatOrb4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-50px, -40px) scale(1.1); }
}

@media (max-width: 768px) {
  .orb-1 { width: 250px; height: 250px; }
  .orb-2 { width: 200px; height: 200px; }
  .orb-3 { width: 150px; height: 150px; }
  .orb-4 { width: 120px; height: 120px; }
}
</style>
