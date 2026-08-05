import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { VRMLoaderPlugin } from '@pixiv/three-vrm'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

class AvatarScene {
  constructor(container) {
    this.container = container
    this.scene = null
    this.camera = null
    this.renderer = null
    this.controls = null
    this.animationId = null
    this.clock = new THREE.Clock()
    this.resizeHandler = this.resize.bind(this)
    this.animateHandler = this.animate.bind(this)
    this.meshes = []
    this.currentVrm = null
    this.loader = null
    this.loading = false
    this.onLoadCallback = null
    this.onErrorCallback = null
    this.currentExpression = null
  }

  init() {
    const width = this.container.clientWidth
    const height = this.container.clientHeight

    this.scene = new THREE.Scene()
    this.scene.background = new THREE.Color(0x16213e)

    this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
    this.camera.position.set(0, 1.5, 3)
    this.camera.lookAt(0, 0, 0)

    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true
    })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setSize(width, height)
    this.renderer.shadowMap.enabled = true
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
    this.container.appendChild(this.renderer.domElement)

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
    this.scene.add(ambientLight)

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
    directionalLight.position.set(5, 10, 7)
    directionalLight.castShadow = true
    directionalLight.shadow.mapSize.width = 1024
    directionalLight.shadow.mapSize.height = 1024
    this.scene.add(directionalLight)

    const pointLight = new THREE.PointLight(0x667eea, 1, 100)
    pointLight.position.set(-3, 2, -3)
    this.scene.add(pointLight)

    const fillLight = new THREE.PointLight(0xf093fb, 0.6, 100)
    fillLight.position.set(3, 1, -3)
    this.scene.add(fillLight)

    const rimLight = new THREE.PointLight(0x764ba2, 0.8, 100)
    rimLight.position.set(0, 3, -5)
    this.scene.add(rimLight)

    this._createPlaceholderAvatar()

    const planeGeo = new THREE.PlaneGeometry(20, 20)
    const planeMat = new THREE.MeshStandardMaterial({
      color: 0x1a1a2e,
      roughness: 0.9,
      metalness: 0.1
    })
    const plane = new THREE.Mesh(planeGeo, planeMat)
    plane.rotation.x = -Math.PI / 2
    plane.position.y = -1
    plane.receiveShadow = true
    this.scene.add(plane)

    this.controls = new OrbitControls(this.camera, this.renderer.domElement)
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.05
    this.controls.minDistance = 0.5
    this.controls.maxDistance = 10
    this.controls.target.set(0, 1, 0)
    this.controls.autoRotate = true
    this.controls.autoRotateSpeed = 1.0

    window.addEventListener('resize', this.resizeHandler)
    this.animate()
  }

  _createPlaceholderAvatar() {
    const group = new THREE.Group()
    group.name = 'placeholder-avatar'

    const bodyGeo = new THREE.SphereGeometry(0.5, 64, 64)
    const bodyMat = new THREE.MeshPhysicalMaterial({
      color: 0x667eea,
      metalness: 0.3,
      roughness: 0.4,
      clearcoat: 0.8,
      clearcoatRoughness: 0.1,
      sheen: 0.5,
      sheenColor: new THREE.Color(0xf093fb)
    })
    const body = new THREE.Mesh(bodyGeo, bodyMat)
    body.position.y = 1.2
    body.castShadow = true
    body.receiveShadow = true
    group.add(body)
    this.meshes.push(body)

    const torusGeo = new THREE.TorusGeometry(0.75, 0.03, 16, 100)
    const torusMat = new THREE.MeshStandardMaterial({
      color: 0x764ba2,
      metalness: 0.8,
      roughness: 0.2,
      emissive: 0x2200aa,
      emissiveIntensity: 0.3
    })
    const torus = new THREE.Mesh(torusGeo, torusMat)
    torus.position.y = 1.2
    torus.rotation.x = Math.PI / 3
    torus.castShadow = true
    group.add(torus)
    this.meshes.push(torus)

    const ringGeo = new THREE.RingGeometry(0.85, 0.88, 100)
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0x667eea,
      transparent: true,
      opacity: 0.4,
      side: THREE.DoubleSide
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.position.y = 1.2
    ring.rotation.x = Math.PI / 2
    group.add(ring)
    this.meshes.push(ring)

    this.scene.add(group)
    this.placeholderGroup = group
  }

  async loadVRM(url) {
    if (this.loading) return
    this.loading = true

    try {
      if (!this.loader) {
        this.loader = new GLTFLoader()
        this.loader.register((parser) => new VRMLoaderPlugin(parser))
      }

      // Convert relative URL to absolute URL
      const absoluteUrl = this._getAbsoluteUrl(url)
      console.log('Loading VRM from:', absoluteUrl)
      
      const response = await fetch(absoluteUrl)
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      
      const arrayBuffer = await response.arrayBuffer()
      console.log('VRM file downloaded, size:', arrayBuffer.byteLength, 'bytes')
      
      const gltf = await this.loader.parseAsync(arrayBuffer, '')
      const vrm = gltf.userData.vrm
      if (!vrm) {
        throw new Error('加载的模型不是VRM格式')
      }

      this._clearAvatar()

      this.currentVrm = vrm
      vrm.scene.position.y = 0
      vrm.scene.rotation.y = Math.PI
      vrm.scene.scale.setScalar(1)

      this.scene.add(vrm.scene)

      this._fitCameraToVRM(vrm)

      if (this.onLoadCallback) {
        this.onLoadCallback(vrm)
      }
    } catch (err) {
      console.error('VRM加载失败:', err)
      if (this.onErrorCallback) {
        this.onErrorCallback(err)
      }
    } finally {
      this.loading = false
    }
  }

  _getAbsoluteUrl(url) {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url
    }
    const baseUrl = window.location.origin
    return baseUrl + url
  }

  _clearAvatar() {
    if (this.placeholderGroup) {
      this.scene.remove(this.placeholderGroup)
      this._disposeObject(this.placeholderGroup)
      this.placeholderGroup = null
    }
    this.meshes = []

    if (this.currentVrm) {
      this.scene.remove(this.currentVrm.scene)
      // 释放 VRM 模型的所有资源
      this._disposeObject(this.currentVrm.scene)
      // 如果有 animationMixer，也要释放
      if (this.currentVrm.animationMixer) {
        this.currentVrm.animationMixer.stopAllAction()
      }
      this.currentVrm = null
    }
  }

  _fitCameraToVRM(vrm) {
    const box = new THREE.Box3().setFromObject(vrm.scene)
    const size = box.getSize(new THREE.Vector3())
    const center = box.getCenter(new THREE.Vector3())

    const maxDim = Math.max(size.x, size.y, size.z)
    const fitHeightDistance = maxDim / (2 * Math.atan((Math.PI * this.camera.fov) / 360))

    this.camera.near = 0.01
    this.camera.far = 1000
    this.camera.updateProjectionMatrix()

    this.camera.position.set(center.x, center.y + size.y / 2, center.z + fitHeightDistance * 0.8)
    this.camera.lookAt(center)

    this.controls.target.copy(center)
    this.controls.update()
  }

  setExpression(name, value = 1) {
    if (!this.currentVrm || !this.currentVrm.expressionManager) return
    try {
      this.currentVrm.expressionManager.setValue(name, value)
      this.currentExpression = name
    } catch (e) {
      console.warn(`设置表情 ${name} 失败:`, e)
    }
  }

  setLookAt(position) {
    if (!this.currentVrm || !this.currentVrm.lookAt) return
    try {
      this.currentVrm.lookAt.target.position.copy(position)
    } catch (e) {
      console.warn('设置lookAt失败:', e)
    }
  }

  setBlendShape(clip, weight) {
    if (!this.currentVrm || !this.currentVrm.expressionManager) return
    try {
      this.currentVrm.expressionManager.setValue(clip, weight)
    } catch (e) {
      console.warn(`设置blendShape ${clip} 失败:`, e)
    }
  }

  playAnimation(animationName) {
    if (!this.currentVrm || !this.currentVrm.humanoid) return
    try {
      const animationMap = {
        idle: 'idle',
        wave: 'wave',
        bow: 'bow'
      }
      const clipName = animationMap[animationName] || animationName
      this.currentVrm.humanoid.playAnimation(clipName)
    } catch (e) {
      console.warn(`播放动画 ${animationName} 失败:`, e)
    }
  }

  onLoad(callback) {
    this.onLoadCallback = callback
  }

  onError(callback) {
    this.onErrorCallback = callback
  }

  animate() {
    this.animationId = requestAnimationFrame(this.animateHandler)

    const elapsed = this.clock.getElapsedTime()

    if (this.currentVrm) {
      this.currentVrm.update(this.clock.getDelta())
    } else {
      this.meshes.forEach((mesh, i) => {
        mesh.rotation.y = elapsed * (0.5 + i * 0.3)
        mesh.rotation.x = Math.sin(elapsed * 0.5 + i) * 0.2
      })

      if (this.meshes[0]) {
        this.meshes[0].position.y = 1.2 + Math.sin(elapsed * 1.5) * 0.1
      }
    }

    this.controls.update()
    this.renderer.render(this.scene, this.camera)
  }

  resize() {
    if (!this.container) return
    const width = this.container.clientWidth
    const height = this.container.clientHeight
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(width, height)
  }

  _disposeObject(obj) {
    if (!obj) return
    obj.traverse((child) => {
      if (child.geometry) {
        child.geometry.dispose()
      }
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach((m) => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
  }

  dispose() {
    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId)
      this.animationId = null
    }

    window.removeEventListener('resize', this.resizeHandler)

    if (this.controls) {
      this.controls.dispose()
      this.controls = null
    }

    this._clearAvatar()

    if (this.renderer) {
      this.renderer.dispose()
      if (this.renderer.domElement && this.renderer.domElement.parentNode) {
        this.renderer.domElement.parentNode.removeChild(this.renderer.domElement)
      }
      this.renderer = null
    }

    if (this.scene) {
      this._disposeObject(this.scene)
      this.scene = null
    }

    this.camera = null
    this.meshes = []
    this.loader = null
  }
}

export default AvatarScene