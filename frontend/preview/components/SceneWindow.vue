<template>
  <svg
    class="scene-window"
    viewBox="0 0 760 640"
    role="img"
    aria-label="原创插画：暮色窗边的角色、悬挂的水晶与星形光斑"
  >
    <defs>
      <linearGradient id="sw-room" x1="0" y1="0" x2="0.8" y2="1">
        <stop offset="0" stop-color="#1c2647" />
        <stop offset="0.52" stop-color="#131c38" />
        <stop offset="1" stop-color="#0b1126" />
      </linearGradient>
      <linearGradient id="sw-sky" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#27305c" />
        <stop offset="0.46" stop-color="#5d5e88" />
        <stop offset="0.74" stop-color="#a987a4" />
        <stop offset="1" stop-color="#d0a9b4" />
      </linearGradient>
      <linearGradient id="sw-window-frame" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#8d8da8" />
        <stop offset="0.5" stop-color="#525a79" />
        <stop offset="1" stop-color="#2e3552" />
      </linearGradient>
      <linearGradient id="sw-hair" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#2a3154" />
        <stop offset="0.55" stop-color="#161c34" />
        <stop offset="1" stop-color="#0d1124" />
      </linearGradient>
      <linearGradient id="sw-knit" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#d3cddb" />
        <stop offset="0.5" stop-color="#a9a4bb" />
        <stop offset="1" stop-color="#6d6c8c" />
      </linearGradient>
      <linearGradient id="sw-skin" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="#f2d1c4" />
        <stop offset="1" stop-color="#c69f9e" />
      </linearGradient>
      <linearGradient id="sw-rim" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#ffe3e6" stop-opacity="0.86" />
        <stop offset="1" stop-color="#f4a9bd" stop-opacity="0.06" />
      </linearGradient>
      <linearGradient id="sw-beam" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="#d7c2da" stop-opacity="0.2" />
        <stop offset="0.55" stop-color="#f2c8d5" stop-opacity="0.08" />
        <stop offset="1" stop-color="#f2c8d5" stop-opacity="0" />
      </linearGradient>
      <linearGradient id="sw-letter" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#fbf5e8" />
        <stop offset="1" stop-color="#d7c8b9" />
      </linearGradient>
      <radialGradient id="sw-moon" cx="0.4" cy="0.35" r="0.7">
        <stop offset="0" stop-color="#fff9ec" />
        <stop offset="0.7" stop-color="#f4e3d5" />
        <stop offset="1" stop-color="#dcbcc2" />
      </radialGradient>
      <radialGradient id="sw-glow" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#ffd0d9" stop-opacity="0.55" />
        <stop offset="1" stop-color="#ffd0d9" stop-opacity="0" />
      </radialGradient>
      <radialGradient id="sw-lamp" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#f3d7a5" stop-opacity="0.32" />
        <stop offset="1" stop-color="#f3d7a5" stop-opacity="0" />
      </radialGradient>
      <filter id="sw-blur-lg" x="-40%" y="-40%" width="180%" height="180%">
        <feGaussianBlur stdDeviation="22" />
      </filter>
      <filter id="sw-blur-sm" x="-40%" y="-40%" width="180%" height="180%">
        <feGaussianBlur stdDeviation="5" />
      </filter>
      <filter id="sw-soften" x="-20%" y="-20%" width="140%" height="140%">
        <feGaussianBlur stdDeviation="1.4" />
      </filter>
    </defs>

    <rect width="760" height="640" rx="28" fill="url(#sw-room)" />

    <!-- room depth and light -->
    <ellipse cx="636" cy="214" rx="188" ry="210" fill="url(#sw-glow)" opacity="0.7" />
    <ellipse cx="154" cy="596" rx="150" ry="38" fill="url(#sw-lamp)" />
    <path d="M212 640 C300 570 470 552 738 604 L760 640 Z" fill="#0a1022" opacity="0.72" />
    <path d="M0 548 C104 520 190 522 238 552 L238 640 L0 640 Z" fill="#0d1428" opacity="0.8" />

    <!-- arched window -->
    <path
      d="M262 586 L262 206 C262 124 322 76 398 76 C474 76 536 124 536 206 L536 586 Z"
      fill="url(#sw-sky)"
    />
    <path
      d="M262 586 L262 206 C262 124 322 76 398 76 C474 76 536 124 536 206 L536 586"
      fill="none"
      stroke="url(#sw-window-frame)"
      stroke-width="10"
    />
    <path
      d="M281 586 L281 210 C281 137 333 94 398 94 C463 94 516 137 516 210 L516 586"
      fill="none"
      stroke="#e5d4d8"
      stroke-width="1.2"
      opacity="0.44"
    />
    <line x1="398" y1="78" x2="398" y2="586" stroke="#737b98" stroke-width="3" opacity="0.7" />
    <line x1="264" y1="342" x2="535" y2="342" stroke="#737b98" stroke-width="3" opacity="0.7" />
    <line x1="281" y1="335" x2="516" y2="335" stroke="#f1dde1" stroke-width="1" opacity="0.28" />

    <!-- outside sky -->
    <circle cx="472" cy="186" r="41" fill="url(#sw-moon)" opacity="0.94" />
    <circle cx="472" cy="186" r="58" fill="#f6d9d9" opacity="0.16" filter="url(#sw-blur-sm)" />
    <path d="M303 434 C336 404 395 400 430 428 C402 445 350 450 303 434Z" fill="#e7c6ce" opacity="0.14" filter="url(#sw-blur-sm)" />
    <path d="M270 420 C322 448 372 452 412 436 C342 470 296 466 270 452 Z" fill="#242b4e" opacity="0.55" />
    <g opacity="0.5" fill="#2a3154">
      <rect x="287" y="454" width="25" height="132" />
      <rect x="319" y="432" width="34" height="154" />
      <rect x="361" y="462" width="29" height="124" />
      <rect x="393" y="444" width="42" height="142" />
      <rect x="444" y="470" width="30" height="116" />
      <rect x="480" y="450" width="38" height="136" />
    </g>
    <g fill="#f7dfe1" opacity="0.8">
      <circle cx="297" cy="507" r="1.4" />
      <circle cx="326" cy="489" r="1.2" />
      <circle cx="371" cy="512" r="1.3" />
      <circle cx="407" cy="478" r="1.5" />
      <circle cx="462" cy="521" r="1.2" />
      <circle cx="501" cy="496" r="1.4" />
    </g>
    <g fill="#fff4e8">
      <path d="M333 142 335.5 151 344.5 153.5 335.5 156 333 165 330.5 156 321.5 153.5 330.5 151Z" opacity="0.72" />
      <path d="M503 256 505 262.5 511.5 264.5 505 266.5 503 273 501 266.5 494.5 264.5 501 262.5Z" opacity="0.56" />
      <path d="M285 302 287 308 293 310 287 312 285 318 283 312 277 310 283 308Z" opacity="0.4" />
    </g>

    <!-- star decals on the glass -->
    <g fill="#e9cfd9" opacity="0.32" filter="url(#sw-soften)">
      <path d="M360 344 374 380 410 394 374 408 360 444 346 408 310 394 346 380Z" />
      <path d="M489 318 499 344 525 354 499 364 489 390 479 364 453 354 479 344Z" />
    </g>
    <path d="M335 356 346 386 376 397 346 408 335 438 324 408 294 397 324 386Z" fill="none" stroke="#ffdde2" stroke-width="1.1" opacity="0.44" />
    <path d="M474 330 483 352 505 361 483 370 474 392 465 370 443 361 465 352Z" fill="none" stroke="#ead9ff" stroke-width="0.9" opacity="0.34" />

    <!-- glass reflections -->
    <path d="M300 120 C348 152 386 238 394 588" fill="none" stroke="#fff4ec" stroke-width="2" opacity="0.16" />
    <path d="M315 112 C364 144 405 240 414 588" fill="none" stroke="#fff4ec" stroke-width="1" opacity="0.12" />

    <!-- light beam -->
    <path d="M300 120 L530 120 L708 576 L370 592 Z" fill="url(#sw-beam)" opacity="0.5" />

    <!-- hanging crystal mobile -->
    <g>
      <line x1="151" y1="0" x2="151" y2="158" stroke="#b99a6f" stroke-width="1.3" opacity="0.62" />
      <ellipse cx="151" cy="166" rx="44" ry="44" fill="none" stroke="#c9ab79" stroke-width="1.3" opacity="0.72" transform="rotate(-20 151 166)" />
      <ellipse cx="152" cy="253" rx="57" ry="57" fill="none" stroke="#c9ab79" stroke-width="1.2" opacity="0.66" transform="rotate(8 152 253)" />
      <ellipse cx="154" cy="354" rx="66" ry="66" fill="none" stroke="#c9ab79" stroke-width="1" opacity="0.5" transform="rotate(-4 154 354)" />
      <line x1="151" y1="166" x2="151" y2="253" stroke="#b99a6f" stroke-width="0.9" opacity="0.55" />
      <line x1="152" y1="253" x2="154" y2="354" stroke="#b99a6f" stroke-width="0.9" opacity="0.48" />
      <!-- glass drop -->
      <path d="M154 291 C166 307 178 329 178 344 C178 363 165 376 154 376 C143 376 130 363 130 344 C130 329 142 307 154 291Z" fill="#e8eef5" opacity="0.18" stroke="#f8f4ed" stroke-width="1.1" />
      <path d="M144 330 C151 340 158 350 158 360 C158 368 152 372 147 370 C141 367 138 358 144 330Z" fill="#fffdf8" opacity="0.36" />
      <ellipse cx="154" cy="386" rx="46" ry="14" fill="url(#sw-glow)" opacity="0.55" />
    </g>

    <!-- floor lamp / books -->
    <g>
      <ellipse cx="211" cy="554" rx="72" ry="62" fill="url(#sw-lamp)" />
      <path d="M196 562 L196 470 C196 454 206 446 217 446 C228 446 238 456 238 470 L238 562Z" fill="#313352" opacity="0.68" />
      <rect x="178" y="545" width="92" height="10" rx="3" fill="#2a2f4a" />
      <rect x="183" y="530" width="82" height="10" rx="3" fill="#47415c" opacity="0.85" />
      <rect x="192" y="518" width="66" height="9" rx="3" fill="#8b6672" opacity="0.52" />
      <path d="M258 540 C252 520 258 502 272 496 L284 500 C296 506 300 524 292 540 C282 552 266 552 258 540Z" fill="#e9e2d4" opacity="0.64" />
      <ellipse cx="274" cy="497" rx="16" ry="5" fill="#f4e6c7" opacity="0.26" />
    </g>

    <!-- seated figure, original vector illustration -->
    <g>
      <!-- hair silhouette -->
      <path d="M547 318 C540 260 567 218 612 214 C653 210 682 242 680 292 C690 336 678 382 660 400 C662 352 650 334 628 330 L586 338 C562 342 550 333 547 318Z" fill="url(#sw-hair)" />
      <path d="M564 312 C550 356 548 414 558 454 C540 438 534 382 545 328Z" fill="#151b32" opacity="0.9" />
      <path d="M667 286 C678 326 676 366 664 398 C684 376 690 330 681 288Z" fill="#0e1327" opacity="0.9" />
      <!-- profile sliver and earring -->
      <path d="M573 288 C580 250 600 236 618 240 C607 252 599 270 597 290 C592 303 581 303 573 288Z" fill="url(#sw-skin)" opacity="0.68" />
      <circle cx="588" cy="326" r="3.2" fill="#faf1e3" opacity="0.9" />
      <circle cx="588" cy="326" r="7" fill="#ffd9df" opacity="0.16" filter="url(#sw-blur-sm)" />
      <!-- knit body -->
      <path d="M498 640 C494 556 514 440 556 400 C584 374 638 368 678 388 C712 406 732 470 738 640Z" fill="url(#sw-knit)" />
      <path d="M516 640 C514 548 534 452 570 414 C602 382 654 378 690 402" fill="none" stroke="#f4ecf2" stroke-width="1.2" opacity="0.34" />
      <g fill="none" stroke="#5c5b76" stroke-width="2.1" opacity="0.4">
        <path d="M552 640 C552 548 566 476 592 434" />
        <path d="M578 640 C578 548 592 486 616 446" />
        <path d="M606 640 C608 554 622 496 642 460" />
        <path d="M636 640 C640 560 652 512 668 478" />
        <path d="M666 640 C672 572 684 530 696 504" />
        <path d="M696 640 C704 586 714 548 722 528" />
      </g>
      <g fill="none" stroke="#f8edf0" stroke-width="1" opacity="0.2">
        <path d="M566 638 C566 548 580 484 604 444" />
        <path d="M622 638 C624 554 638 504 654 470" />
        <path d="M682 638 C688 576 698 540 706 518" />
      </g>
      <!-- arm holding letter -->
      <path d="M598 472 C622 448 664 444 692 468 C708 482 710 514 696 534 C670 556 624 552 600 524 C590 512 588 484 598 472Z" fill="#c2bccd" />
      <path d="M602 476 C628 456 660 454 684 474" fill="none" stroke="#fbf1f2" stroke-width="1.1" opacity="0.28" />
      <g transform="rotate(-8 636 506)">
        <rect x="598" y="478" width="82" height="54" rx="3" fill="url(#sw-letter)" />
        <rect x="604" y="484" width="70" height="42" rx="2" fill="none" stroke="#a79183" stroke-width="0.7" opacity="0.5" />
        <line x1="613" y1="495" x2="666" y2="495" stroke="#8e7c77" stroke-width="1" opacity="0.45" />
        <line x1="613" y1="504" x2="657" y2="504" stroke="#8e7c77" stroke-width="1" opacity="0.36" />
        <line x1="613" y1="513" x2="662" y2="513" stroke="#8e7c77" stroke-width="1" opacity="0.3" />
      </g>
      <path d="M661 514 C675 510 686 515 689 524 C690 532 682 537 673 533 C666 530 660 522 661 514Z" fill="url(#sw-skin)" opacity="0.7" />
      <!-- rim light from the window -->
      <path d="M556 400 C552 452 546 518 552 640" fill="none" stroke="url(#sw-rim)" stroke-width="4.2" opacity="0.8" filter="url(#sw-soften)" />
      <path d="M574 300 C578 270 596 246 620 232" fill="none" stroke="#ffdedd" stroke-width="2.2" opacity="0.46" filter="url(#sw-soften)" />
    </g>

    <!-- letter glow and dust -->
    <ellipse cx="636" cy="505" rx="78" ry="54" fill="#f3dedf" opacity="0.09" filter="url(#sw-blur-lg)" />
    <g fill="#fff6ec" opacity="0.5">
      <circle cx="476" cy="462" r="1.6" />
      <circle cx="505" cy="428" r="1.1" />
      <circle cx="518" cy="510" r="1.4" />
      <circle cx="448" cy="530" r="1.2" />
      <circle cx="706" cy="352" r="1.5" />
      <circle cx="258" cy="396" r="1.2" />
      <circle cx="620" cy="586" r="1.3" />
    </g>

    <!-- window sill -->
    <rect x="244" y="586" width="310" height="8" rx="3" fill="#383f5d" opacity="0.9" />
    <rect x="244" y="586" width="310" height="2" rx="1" fill="#f1dde2" opacity="0.3" />
  </svg>
</template>

<style scoped>
.scene-window {
  width: 100%;
  height: auto;
  border-radius: 26px;
  box-shadow: var(--dream-shadow-deep);
}
</style>
