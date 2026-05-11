import {Config} from '@docusaurus/types';
import * as preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'MtdrSpring',
  tagline: 'Backend and deployment documentation for the MtdrSpring Spring Boot service',
  favicon: 'img/favicon.ico',

  // Production URL
  url: 'https://your-domain.com',
  baseUrl: '/',

  organizationName: 'your-org',
  projectName: 'mtdrspring',

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/your-org/mtdrspring/tree/main/',
        },
        blog: {
          showReadingTime: true,
          editUrl: 'https://github.com/your-org/mtdrspring/tree/main/',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } as typeof preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/mtdrspring-social-card.jpg',
    navbar: {
      title: 'MtdrSpring',
      logo: {
        alt: 'MtdrSpring Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          href: 'https://github.com/your-org/mtdrspring',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/intro',
            },
            {
              label: 'Deployment',
              to: '/docs/deployment/overview',
            },
          ],
        },
        {
          title: 'Resources',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/your-org/mtdrspring',
            },
            {
              label: 'OCI Console',
              href: 'https://www.oracle.com/cloud/',
            },
          ],
        },
      ],
      copyright: `Copyright © 2026 MtdrSpring. Built with Docusaurus.`,
    },
    prism: {
      theme: {
        plain: {
          color: '#383A42',
          backgroundColor: '#FAFAFA',
        },
        styles: [],
      },
      darkTheme: {
        plain: {
          color: '#ABB2BF',
          backgroundColor: '#282C34',
        },
        styles: [],
      },
    },
  },
};

export default config;
