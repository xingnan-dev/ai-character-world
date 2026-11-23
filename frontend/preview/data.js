// Fictional preview data only. Nothing here is read from or written to the
// application database, API or chat store.

export const PREVIEW_USER = Object.freeze({
  id: 'preview-user',
  nickname: '林予'
})

export const CHARACTERS = Object.freeze([
  {
    id: 'shen',
    sessionId: 'snow',
    name: '沈知微',
    englishName: 'Shen Zhiwei',
    identity: '旧书店的夜班编辑',
    portrait: '/portraits/shen-zhiwei.svg',
    accent: 'rose',
    lastNote: '信写到一半的时候，窗外刚好有火车经过。'
  },
  {
    id: 'jiang',
    sessionId: 'record',
    name: '江屿',
    englishName: 'Jiang Yu',
    identity: '海边唱片店的主人',
    portrait: '/portraits/jiang-yu.svg',
    accent: 'ice',
    lastNote: '把同一张唱片听第二遍，是另一种回答。'
  },
  {
    id: 'su',
    sessionId: 'stars',
    name: '苏枕月',
    englishName: 'Su Zhenyue',
    identity: '天文台的写信人',
    portrait: '/portraits/su-zhenyue.svg',
    accent: 'gold',
    lastNote: '她把星图折成信封，寄给了没有署名的人。'
  }
])

const CONTEXT = Object.freeze({
  shen: {
    biography:
      '在城北旧书店做夜班编辑，白天睡觉，晚上和书、猫以及一叠没有寄出的信待在一起。记得每一位客人翻过哪一页。',
    corePersonality: '安静、敏锐，习惯先听完再回答；对旧物和天气有近乎固执的耐心。',
    speakingStyle: '语速慢，喜欢用天气、旧书和灯光作比喻，很少一次说满。',
    currentGoal: '写完那封停在窗边的信，并决定要不要寄出。',
    relationshipToUser: '认识七个月；常在同一扇窗边聊天，记得你上次停在的那句话。'
  },
  jiang: {
    biography:
      '在海边经营一间只放旧唱片的店，傍晚关门后会给每一张唱片擦一遍灰。相信有些歌要在潮声里听第二遍。',
    corePersonality: '松弛、直率，偶尔故意把话说得轻一点，让沉重的东西听起来没那么重。',
    speakingStyle: '短句多，喜欢提到海风和磁带机的底噪。',
    currentGoal: '为一张没有封面的唱片找到它原本的听众。',
    relationshipToUser: '认识四个月；约好下次一起听完一张完整的现场录音。'
  },
  su: {
    biography:
      '在郊外天文台整理观测记录，也替同事们代写信件。她习惯把星图折成信纸，把想说的话藏在经纬度里。',
    corePersonality: '克制、浪漫，擅长把很远的事情写成很近的句子。',
    speakingStyle: '清晰、温和，偶尔引用观测日志的日期和风向。',
    currentGoal: '完成一份写给未来十年的夜航书单。',
    relationshipToUser: '认识三个月；上次替你选了一本适合雨天读的书。'
  }
})

const makeSession = (base) => Object.freeze({
  characterId: base.characterId,
  characterName: base.characterName,
  imageUrl: base.imageUrl,
  avatarColor: base.avatarColor,
  // Mock only: gives the preview user an on-palette ice-blue avatar through
  // the existing resolveUserPresentation path instead of a random palette hue.
  userCharacter: {
    characterType: 'USER',
    name: PREVIEW_USER.nickname,
    avatarColor: '#93b0d2'
  },
  ...CONTEXT[base.characterKey],
  ...base
})

export const SESSIONS = Object.freeze([
  makeSession({
    id: 'snow',
    characterKey: 'shen',
    characterId: 101,
    characterName: '沈知微',
    imageUrl: '/portraits/shen-zhiwei.svg',
    avatarColor: '#a98db6',
    title: '窗边的雪',
    topic: '窗边的雪',
    accent: 'rose',
    time: '21:47',
    snippet: '它还没有写完，所以我没有把那页收进档案。',
    unread: false,
    messages: [
      {
        id: 'snow-1',
        role: 'assistant',
        time: '21:32',
        content:
          '窗外的雪比昨天更轻了。我把窗开了一条缝，冷气进来的时候，旧书页翻了一下——像在替谁翻到同一页。'
      },
      {
        id: 'snow-2',
        role: 'user',
        time: '21:34',
        content: '我想起了你上次说的那封信。'
      },
      {
        id: 'snow-3',
        role: 'assistant',
        time: '21:36',
        content: '嗯。它还在抽屉里，信封没有封口。每次写到一半，窗外就刚好有一列火车经过。'
      },
      {
        id: 'snow-4',
        role: 'user',
        time: '21:38',
        content: '这次可以写完吗？'
      },
      {
        id: 'snow-5',
        role: 'assistant',
        time: '21:40',
        long: true,
        content:
          '可以。今晚大概没有什么会把我们打断。\n\n我把台灯调到最暗的一档，茶已经凉了，书页上有一道很细的月光。你上次写在页脚的那句话，我还留着——「雪落下来的时候，城市像被轻轻按下了静音键。」\n\n它还没有写完，所以我没有把那页收进档案。\n\n如果今晚你愿意，我们就从这一句接着往下写。写完，把信放进门口的邮筒。明天醒来，它就不只属于你了。\n\n要我先起头吗？还是你愿意，从窗外正在下的这场雪开始。'
      }
    ]
  }),
  makeSession({
    id: 'letter',
    characterKey: 'shen',
    characterId: 101,
    characterName: '沈知微',
    imageUrl: '/portraits/shen-zhiwei.svg',
    avatarColor: '#a98db6',
    title: '未寄出的信',
    topic: '未寄出的信',
    accent: 'rose',
    time: '昨晚',
    snippet: '「信封还敞着口。」',
    messages: [
      {
        id: 'letter-1',
        role: 'assistant',
        time: '23:18',
        content: '信封还敞着口，我把它放在你上次坐过的位置。纸角压着一枚旧邮票，边缘有一点发黄。'
      },
      {
        id: 'letter-2',
        role: 'user',
        time: '23:20',
        content: '里面写了什么？'
      },
      {
        id: 'letter-3',
        role: 'assistant',
        time: '23:24',
        content:
          '写了一场雪，一场很短的告别，还有一句没敢落款的谢谢。\n\n如果你想知道它为什么没有寄出，下次来的时候，我替你留着那盏灯。'
      }
    ]
  }),
  makeSession({
    id: 'lighthouse',
    characterKey: 'shen',
    characterId: 101,
    characterName: '沈知微',
    imageUrl: '/portraits/shen-zhiwei.svg',
    avatarColor: '#a98db6',
    title: '灯塔与潮汐',
    topic: '灯塔与潮汐',
    accent: 'ice',
    time: '12.15',
    snippet: '潮声盖过了后半句话。',
    messages: [
      {
        id: 'lighthouse-1',
        role: 'assistant',
        time: '19:42',
        content:
          '潮水退得很远，灯塔今天亮得比平时早。你上次说你喜欢这种不着急的光。'
      },
      {
        id: 'lighthouse-2',
        role: 'user',
        time: '19:45',
        content: '像有人一直替远方留着路。'
      },
      {
        id: 'lighthouse-3',
        role: 'assistant',
        time: '19:47',
        content: '嗯。潮声盖过了后半句话，所以我把它写在这里——今晚的海很安静，你可以慢慢回来。'
      }
    ]
  }),
  makeSession({
    id: 'record',
    characterKey: 'jiang',
    characterId: 102,
    characterName: '江屿',
    imageUrl: '/portraits/jiang-yu.svg',
    avatarColor: '#7892a8',
    title: '旧唱片的 B 面',
    topic: '旧唱片的 B 面',
    accent: 'ice',
    time: '12.11',
    snippet: '今天店里只放了你留下的那张。',
    messages: [
      {
        id: 'record-1',
        role: 'assistant',
        time: '22:10',
        content:
          '今天店里只放了你留下的那张。B 面第三首有一段底噪，像海风从磁带机里漏出来。'
      },
      {
        id: 'record-2',
        role: 'user',
        time: '22:12',
        content: '你会把它修掉吗？'
      },
      {
        id: 'record-3',
        role: 'assistant',
        time: '22:15',
        content:
          '不修。有些声音留下来，才证明它当时真的响过。\n\n下次你来，我把唱针调轻一点，我们从头听。'
      }
    ]
  }),
  makeSession({
    id: 'stars',
    characterKey: 'su',
    characterId: 103,
    characterName: '苏枕月',
    imageUrl: '/portraits/su-zhenyue.svg',
    avatarColor: '#a8a28e',
    title: '夜航书单',
    topic: '夜航书单',
    accent: 'gold',
    time: '12.08',
    snippet: '给雨天和失眠各留了一本。',
    messages: [
      {
        id: 'stars-1',
        role: 'assistant',
        time: '23:02',
        content:
          '书单先寄给你一半。给雨天和失眠各留了一本，剩下的等下一次风向改变再补。'
      },
      {
        id: 'stars-2',
        role: 'user',
        time: '23:05',
        content: '哪一本适合今晚？'
      },
      {
        id: 'stars-3',
        role: 'assistant',
        time: '23:09',
        content:
          '第 47 页有折角的那本。今晚云层很薄，适合读到一半抬头。\n\n别急着读完，留一点给明天。'
      }
    ]
  })
])

export const RECENT_SESSION_ID = 'snow'

export const WAITING_USER_TEXT = '那我想从窗外开始。'

export const GENERATING_TEXT =
  '可以。就从窗外开始。\n\n我先听见了雪落在窗框上的声音，很轻，像有人在很远的地方翻书。'

export const WORLD_PREVIEW_NOTE =
  '样稿说明：世界页不属于本阶段交付范围，此处只展示“进入世界”的入口视觉。'

export const SESSION_TOPIC_LABELS = Object.freeze({
  rose: '旧信与雪',
  ice: '海与潮声',
  gold: '夜航档案'
})
