import type {SidebarsConfig} from '@docusaurus/types';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    {
      type: 'category',
      label: '📚 Getting Started',
      items: [
        'intro',
        'quick-start',
      ],
    },
    {
      type: 'category',
      label: '🏗️ Architecture',
      items: [
        'architecture/overview',
        'architecture/system-design',
        'architecture/database',
      ],
    },
    {
      type: 'category',
      label: '🔧 Backend',
      items: [
        'backend/setup',
        'backend/structure',
        'backend/api-reference',
        'backend/spring-boot-config',
      ],
    },
    {
      type: 'category',
      label: '🚀 Deployment',
      items: [
        'deployment/overview',
        'deployment/local-development',
        'deployment/docker-setup',
        'deployment/oci-deployment',
        'deployment/scripts-reference',
        'deployment/troubleshooting',
      ],
    },
    {
      type: 'category',
      label: '📖 Guides',
      items: [
        'guides/database-management',
        'guides/network-configuration',
        'guides/monitoring-logging',
        'guides/scaling-performance',
        'guides/security-hardening',
      ],
    },
    {
      type: 'doc',
      id: 'faq',
      label: '❓ FAQ',
    },
  ],
};

export default sidebars;
