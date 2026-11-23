// Fictional preview data only. Nothing here is read from or written to the
// application database, API or chat store.

import portraitAli from './assets/art/portrait-ali.webp'
import portraitShen from './assets/art/portrait-shen-zhiwei.webp'
import portraitJiang from './assets/art/portrait-jiang-yu.webp'
import portraitSu from './assets/art/portrait-su.webp'

export const PREVIEW_USER = Object.freeze({
  id: 'preview-user',
  nickname: '林予'
})

export const CHARACTERS = Object.freeze([
  {
    id: 'ali',
    sessionId: 'tomato',
    name: '阿梨',
    englishName: 'Ali',
    identity: '植物工作室的主理人',
    portrait: portraitAli,
    accent: 'sun',
    lastNote: '「今天有没有一件小事，值得记下来？」'
  },
  {
    id: 'shen',
    sessionId: 'rain',
    name: '沈知微',
    englishName: 'Shen Zhiwei',
    identity: '旧书店的夜班编辑',
    portrait: portraitShen,
    accent: 'rose',
    lastNote: '她把你停下的那一页，夹进了一枚干花。'
  },
  {
    id: 'jiang',
    sessionId: 'record',
    name: '江屿',
    englishName: 'Jiang Yu',
    identity: '海边唱片店的主人',
    portrait: portraitJiang,
    accent: 'ice',
    lastNote: '他说同一首歌，换一根唱针会多出一点呼吸。'
  },
  {
    id: 'su',
    sessionId: 'rooftop',
    name: '苏枕月',
    englishName: 'Su Zhenyue',
    identity: '顶楼气象台的记录员',
    portrait: portraitSu,
    accent: 'violet',
    lastNote: '风向转西之后，她把顶楼的窗全打开了。'
  }
])

const CONTEXT = Object.freeze({
  ali: {
    biography:
      '在城南开一间很小的植物工作室，替人照顾养不好的绿植，也做花束。说话很快，笑起来更响，喜欢把平常日子拆成一件一件值得高兴的小事。',
    corePersonality: '热络、直接、有一点急性子；不擅长绕弯子，但很会把人从低落里拎出来。',
    speakingStyle: '短句、口头语多，爱用感叹号，偶尔突然认真起来。',
    currentGoal: '给工作室的雨季排班留出一个能晒太阳的空位。',
    relationshipToUser: '认识五个月；你上次留在店里的纸杯，她还放在木架上。'
  },
  shen: {
    biography:
      '在城北旧书店做夜班编辑，白天睡觉，晚上和书、猫以及一叠书签待在一起。记得每一位客人翻过哪一页。',
    corePersonality: '安静、敏锐，习惯先听完再回答；对旧物和天气有近乎固执的耐心。',
    speakingStyle: '语速慢，喜欢用天气、旧书和灯光作比喻，很少一次说满。',
    currentGoal: '把书店窗边的阅读位重新整理好，留一个给常来的人。',
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
      '在城市顶楼的气象站整理观测记录，也替同事们代写信件。她习惯把风向、云量和日期夹进信里，把很远的事情写成很近的句子。',
    corePersonality: '克制、好奇，擅长在平常的记录里发现浪漫；不喜欢夸张，但很会留意细节。',
    speakingStyle: '清晰、温和，偶尔引用观测日志的日期和风向。',
    currentGoal: '完成一份给雨天的城市散步清单。',
    relationshipToUser: '认识三个月；上次替你记下了适合出门的风向和时段。'
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
    avatarColor: '#8fb2cd'
  },
  ...CONTEXT[base.characterKey],
  ...base
})

export const SESSIONS = Object.freeze([
  makeSession({
    id: 'tomato',
    characterKey: 'ali',
    characterId: 201,
    characterName: '阿梨',
    imageUrl: portraitAli,
    avatarColor: '#cf9674',
    title: '番茄的第一颗果',
    topic: '番茄的第一颗果',
    accent: 'sun',
    time: '22:10',
    snippet: '「我替你留着窗边那张桌子。」',
    unread: false,
    messages: [
      {
        id: 'tomato-1',
        role: 'assistant',
        time: '21:52',
        content:
          '好消息！窗台那盆番茄今天红了第一颗。我忍着没摘，给它拍了张照片，等你来再决定先吃还是先拍照。'
      },
      {
        id: 'tomato-2',
        role: 'user',
        time: '21:54',
        content: '先拍照！它等了这么久。'
      },
      {
        id: 'tomato-3',
        role: 'assistant',
        time: '21:56',
        content:
          '好，听你的。我把它摆在木架上，旁边放了你上次留下的纸杯。它有点歪，但歪得很神气。\n\n对了，你上次说想换个能晒太阳的座位。我把靠窗那桌擦干净了，桌上还剩一小截蜡笔，应该是你画的。'
      },
      {
        id: 'tomato-4',
        role: 'user',
        time: '21:58',
        content: '那是我画的你。'
      },
      {
        id: 'tomato-5',
        role: 'assistant',
        time: '22:10',
        long: true,
        content:
          '我就说那两条线不像番茄。\n\n那我不擦了，给它罩个杯垫。你下次来，可以从门口先看见它——我把叶子修得高了点，但没舍得剪掉多余的那根侧枝，它现在朝你坐的方向伸。\n\n今天店里来了个小朋友，对着薄荷叶闻了很久，说像牙膏。我差点笑出声。你要是也在，肯定会接着问他要不要尝一口。\n\n对了，晚一点可能会下阵雨。窗边那桌给你留着，伞我也放了一把在门后。来不来都没关系，番茄会等你。\n\n下次想先聊什么？发芽、下雨，还是你今天遇见的怪事。'
      }
    ]
  }),
  makeSession({
    id: 'rain',
    characterKey: 'shen',
    characterId: 202,
    characterName: '沈知微',
    imageUrl: portraitShen,
    avatarColor: '#b58aa0',
    title: '雨停之后',
    topic: '雨停之后',
    accent: 'rose',
    time: '昨晚',
    snippet: '「灯没有关，我一直在整理窗边那排书。」',
    messages: [
      {
        id: 'rain-1',
        role: 'assistant',
        time: '23:12',
        content:
          '雨停得比预报早。我把窗推开一条缝，潮气把旧书的纸边都熏得有点卷。'
      },
      {
        id: 'rain-2',
        role: 'user',
        time: '23:14',
        content: '你还没走？'
      },
      {
        id: 'rain-3',
        role: 'assistant',
        time: '23:18',
        content:
          '灯没有关，我一直在整理窗边那排书。\n\n你上次停在的那一页，我夹了一枚干花。不是催你回来，只是觉得那一页不该空着。'
      },
      {
        id: 'rain-4',
        role: 'user',
        time: '23:20',
        content: '明天我可能路过。'
      },
      {
        id: 'rain-5',
        role: 'assistant',
        time: '23:24',
        content: '好。路过也不用特意说明，窗边的位置会空着。'
      }
    ]
  }),
  makeSession({
    id: 'record',
    characterKey: 'jiang',
    characterId: 203,
    characterName: '江屿',
    imageUrl: portraitJiang,
    avatarColor: '#7497b0',
    title: '唱针调到最轻',
    topic: '唱针调到最轻',
    accent: 'ice',
    time: '12.11',
    snippet: '「今天店里只放了你留下的那张。」',
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
    id: 'rooftop',
    characterKey: 'su',
    characterId: 204,
    characterName: '苏枕月',
    imageUrl: portraitSu,
    avatarColor: '#9289b1',
    title: '顶楼的风向',
    topic: '顶楼的风向',
    accent: 'violet',
    time: '12.08',
    snippet: '「西风，适合把窗全部打开。」',
    messages: [
      {
        id: 'rooftop-1',
        role: 'assistant',
        time: '18:40',
        content:
          '今天顶楼的风向终于转西，云量很低。我把记录本翻到新的一页，顺便替你标了一个适合散步的时段。'
      },
      {
        id: 'rooftop-2',
        role: 'user',
        time: '18:43',
        content: '几点？'
      },
      {
        id: 'rooftop-3',
        role: 'assistant',
        time: '18:46',
        content:
          '七点以后。太阳落下去，但屋顶还留一点余温。\n\n不用带什么，走到风变凉就回来。'
      }
    ]
  }),
  makeSession({
    id: 'snack',
    characterKey: 'ali',
    characterId: 201,
    characterName: '阿梨',
    imageUrl: portraitAli,
    avatarColor: '#cf9674',
    title: '夜市的第二碗冰粉',
    topic: '夜市的第二碗冰粉',
    accent: 'sun',
    time: '12.06',
    snippet: '「第二碗是你请的，我记得。」',
    messages: [
      {
        id: 'snack-1',
        role: 'assistant',
        time: '20:22',
        content:
          '昨晚那家冰粉摊又来了，还是同一对老夫妻。第二碗是你请的，我记得。'
      },
      {
        id: 'snack-2',
        role: 'user',
        time: '20:24',
        content: '下次换我请。'
      },
      {
        id: 'snack-3',
        role: 'assistant',
        time: '20:27',
        content:
          '成交。\n\n不过说好了，这次要选红糖味。你上次明明盯着人家桶里的红糖看了很久。'
      }
    ]
  })
])

export const RECENT_SESSION_ID = 'tomato'

export const WAITING_USER_TEXT = '那先听番茄的吧。'

export const GENERATING_TEXT =
  '好，从番茄开始。\n\n它其实昨天夜里就偷偷红了一半，今天早上我拉开窗帘，才看见那一点。'

export const WORLD_PREVIEW_NOTE =
  '样稿说明：世界页不属于本阶段交付范围，此处只展示「微光街区」的入口视觉。'

export const SESSION_TOPIC_LABELS = Object.freeze({
  sun: '植物与日常',
  rose: '旧书与雨声',
  ice: '海与唱针',
  violet: '云量与风向'
})
