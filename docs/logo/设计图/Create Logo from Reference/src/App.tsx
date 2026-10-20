export default function App() {
  return (
    <main className="logo-stage">
      <svg
        className="wordmark"
        viewBox="0 0 1339 339"
        role="img"
        aria-labelledby="wordmark-title wordmark-description"
        xmlns="http://www.w3.org/2000/svg"
      >
        <title id="wordmark-title">CodeNow</title>
        <desc id="wordmark-description">
          CodeNow wordmark in deep blue, cyan and electric blue.
        </desc>

        <defs>
          {/* C：深藏青，底部转亮蓝，顶部一道冷色高光 */}
          <linearGradient id="navy" x1=".05" y1="0" x2=".45" y2="1">
            <stop offset="0"    stopColor="#7d88ab" />
            <stop offset="0.03" stopColor="#2b3560" />
            <stop offset="0.1"  stopColor="#0c1f57" />
            <stop offset="0.2"  stopColor="#071749" />
            <stop offset="0.35" stopColor="#08194a" />
            <stop offset="0.5"  stopColor="#122968" />
            <stop offset="0.62" stopColor="#103187" />
            <stop offset="0.8"  stopColor="#0d2777" />
            <stop offset="1"    stopColor="#10318c" />
          </linearGradient>
          {/* d / e：接近平涂的深藏青，顶部高光略强一些 */}
          <linearGradient id="navy-deep" x1="0" y1="0" x2=".2" y2="1">
            <stop offset="0"    stopColor="#9aa3c0" />
            <stop offset="0.06" stopColor="#3a4570" />
            <stop offset="0.13" stopColor="#0a1c55" />
            <stop offset="0.3"  stopColor="#081a4f" />
            <stop offset="0.6"  stopColor="#07184a" />
            <stop offset="1"    stopColor="#08194b" />
          </linearGradient>
          {/* 第一个 o：青色渐变 + 克制的斜向高光带 */}
          <linearGradient
            id="cyan"
            x1="200"
            y1="100"
            x2="340"
            y2="262"
            gradientUnits="userSpaceOnUse"
          >
            <stop offset="0"     stopColor="#01d4d2" />
            <stop offset="0.2"   stopColor="#00cbd2" />
            <stop offset="0.4"   stopColor="#00bfd1" />
            <stop offset="0.465" stopColor="#14cdd4" />
            <stop offset="0.49"  stopColor="#5fdedb" />
            <stop offset="0.515" stopColor="#17cbd3" />
            <stop offset="0.58"  stopColor="#00b6d1" />
            <stop offset="0.78"  stopColor="#01a3ce" />
            <stop offset="1"     stopColor="#0096d1" />
          </linearGradient>
          {/* 第二个 o：青色渐变 + 克制的斜向高光带 */}
          <linearGradient
            id="cyan-two"
            x1="885"
            y1="98"
            x2="1045"
            y2="262"
            gradientUnits="userSpaceOnUse"
          >
            <stop offset="0"     stopColor="#00d4d2" />
            <stop offset="0.24"  stopColor="#01cdd3" />
            <stop offset="0.42"  stopColor="#00c9d3" />
            <stop offset="0.475" stopColor="#18ced4" />
            <stop offset="0.5"   stopColor="#63dfdd" />
            <stop offset="0.53"  stopColor="#16ccd3" />
            <stop offset="0.62"  stopColor="#02a8d2" />
            <stop offset="0.8"   stopColor="#0097d1" />
            <stop offset="1"     stopColor="#0097d1" />
          </linearGradient>
          <linearGradient
            id="bolt-light"
            x1="864"
            y1="78"
            x2="742"
            y2="238"
            gradientUnits="userSpaceOnUse"
          >
            <stop offset="0"    stopColor="#2a86fd" />
            <stop offset="0.5"  stopColor="#1a74fd" />
            <stop offset="1"    stopColor="#2a86fd" />
          </linearGradient>
          {/* N 与 w 共用：上下对称的蓝色渐变 */}
          <linearGradient id="blue-word" x1="0" y1="0" x2=".72" y2="1">
            <stop offset="0"    stopColor="#0a55fb" />
            <stop offset="0.25" stopColor="#0d5efd" />
            <stop offset="0.5"  stopColor="#0c5dfe" />
            <stop offset="0.75" stopColor="#0d5efd" />
            <stop offset="1"    stopColor="#0a55fb" />
          </linearGradient>
          <filter
            id="soft"
            x="-4%"
            y="-8%"
            width="108%"
            height="116%"
            colorInterpolationFilters="sRGB"
          >
            <feGaussianBlur
              in="SourceAlpha"
              stdDeviation="1.25"
              result="blur"
            />
            <feOffset dy="1.1" result="offset" />
            <feColorMatrix
              in="offset"
              values="0 0 0 0 0.02 0 0 0 0 0.07 0 0 0 0 0.2 0 0 0 .22 0"
            />
            <feBlend in="SourceGraphic" />
          </filter>
        </defs>

        <g filter="url(#soft)">
          <g
            fill="none"
            stroke="url(#navy)"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="36"
          >
            {/* 六边形 "C"：顶点做圆角处理，缺口朝右 */}
            <path d="M165.2 107 134 89Q115 78 95.9 89L57.9 111Q38.8 122 38.8 144V188Q38.8 210 57.9 221L95.9 243Q115 254 134 243L165.2 225" />
          </g>

          <path
            transform="translate(-14 0)"
            fill="url(#cyan)"
            fillRule="evenodd"
            d="M277 110c48 0 84 33 84 76 0 45-36 77-84 77s-83-32-83-77c0-43 35-76 83-76Zm0 38c-4 0-8 2-12 6l-21 21c-7 7-7 16 0 23l21 21c7 7 17 7 24 0l21-21c7-7 7-16 0-23l-21-21c-4-4-8-6-12-6Z"
          />

          <path
            fill="url(#navy-deep)"
            fillRule="evenodd"
            d="M458 79c0-1 2-2 4-2h40c3 0 5 2 5 5v176h-45l-1-17c-11 13-27 20-47 20-36 0-63-30-63-72 0-43 27-72 63-72 18 0 33 6 44 18V79Zm-48 110c0 20 11 33 27 33 15 0 26-13 26-33s-11-32-26-32c-16 0-27 12-27 32Z"
          />

          <path
            fill="url(#navy-deep)"
            fillRule="evenodd"
            d="M592 116c46 0 73 31 73 75 0 5 0 10-1 15H562c5 13 16 20 33 20 14 0 24-4 35-13l26 27c-15 15-35 22-63 22-48 0-80-30-80-73 0-42 33-73 79-73Zm-30 62h56c-3-15-13-23-27-23s-25 8-29 23Z"
          />

          <g transform="matrix(1 0 -0.14 1 28 0)">
            {/* N 右移 */}
            <g transform="translate(12 0)">
              <path
                fill="url(#blue-word)"
                d="m643 281 76-194c2-5 5-8 10-9l38-9c5-1 8 1 10 6l25 80L931 59 815 250c-2 3-5 5-9 5h-40c-4 0-7-2-8-6l-17-68-98 100Z"
              />
              <path
                fill="url(#bolt-light)"
                d="m802 155 129-96-116 191c-2 3-5 5-9 5h-40c-4 0-7-2-8-6l-7-28 51-66Z"
              />
            </g>

            <path
              fill="url(#cyan-two)"
              fillRule="evenodd"
              d="M973 108c49 0 85 33 85 78 0 46-36 79-85 79-48 0-84-33-84-79 0-45 36-78 84-78Zm0 39c-5 0-9 2-12 6l-22 21c-7 7-7 17 0 24l22 22c7 7 17 7 24 0l22-22c7-7 7-17 0-24l-22-21c-3-4-8-6-12-6Z"
            />

            <path
              fill="url(#blue-word)"
              d="M1056 125c-1-4 2-7 6-8l39-6c4-1 7 2 8 6l12 85 29-81c1-3 4-5 7-5h38c4 0 6 2 7 6l17 79 37-80c1-3 4-5 7-5h34c5 0 7 4 5 8l-60 132c-1 3-4 5-7 5h-50c-4 0-7-3-8-6l-14-70-27 71c-1 3-4 5-7 5h-49c-4 0-7-3-8-7l-16-129Z"
            />
          </g>

        </g>
      </svg>
    </main>
  )
}
