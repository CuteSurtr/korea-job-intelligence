INSERT INTO sources (code, display_name, adapter_kind, runtime_available, trust_tier,
                     stable_external_id, provides_full_description, enabled, base_url)
VALUES
    ('greenhouse',  'Greenhouse Job Board API', 'ATS',    true,  1, true,  true,  'https://boards-api.greenhouse.io'),
    ('ashby',       'Ashby Job Board API',      'ATS',    true,  1, true,  true,  'https://api.ashbyhq.com'),
    ('lever',       'Lever Postings API',       'ATS',    true,  1, true,  true,  'https://api.lever.co'),
    ('pathsdog',    'Pathsdog',                 'IMPORT', false, 2, true,  true,  'https://jobs.pathsdog.com'),
    ('jobkorea',    'JobKorea',                 'IMPORT', false, 2, true,  false, 'https://www.jobkorea.co.kr'),
    ('saramin',     'Saramin',                  'IMPORT', false, 2, true,  false, 'https://www.saramin.co.kr'),
    ('freehire',    'freehire.me',              'IMPORT', false, 2, true,  true,  'https://freehire.me'),
    ('linkedin',    'LinkedIn Jobs',            'IMPORT', false, 3, true,  false, 'https://www.linkedin.com'),
    ('indeed',      'Indeed',                   'IMPORT', false, 3, false, false, 'https://kr.indeed.com'),
    ('jobdatalake', 'JobDataLake',              'IMPORT', false, 3, false, false, NULL),
    ('manual',      'Manual entry',             'MANUAL', true,  1, true,  true,  NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO source_health (source_id)
SELECT id FROM sources
ON CONFLICT (source_id) DO NOTHING;

INSERT INTO skills (slug, display_name, category, aliases) VALUES
    ('java',           'Java',            'LANGUAGE',  ARRAY['java']),
    ('kotlin',         'Kotlin',          'LANGUAGE',  ARRAY['kotlin']),
    ('python',         'Python',          'LANGUAGE',  ARRAY['python', 'python3']),
    ('typescript',     'TypeScript',      'LANGUAGE',  ARRAY['typescript', 'ts']),
    ('javascript',     'JavaScript',      'LANGUAGE',  ARRAY['javascript', 'js', 'ecmascript']),
    ('go',             'Go',              'LANGUAGE',  ARRAY['golang', 'go lang']),
    ('rust',           'Rust',            'LANGUAGE',  ARRAY['rust']),
    ('csharp',         'C#',              'LANGUAGE',  ARRAY['c#', 'csharp', '.net']),
    ('cpp',            'C++',             'LANGUAGE',  ARRAY['c++', 'cpp']),
    ('c',              'C',               'LANGUAGE',  ARRAY['c language']),
    ('scala',          'Scala',           'LANGUAGE',  ARRAY['scala']),
    ('ruby',           'Ruby',            'LANGUAGE',  ARRAY['ruby']),
    ('php',            'PHP',             'LANGUAGE',  ARRAY['php']),
    ('swift',          'Swift',           'LANGUAGE',  ARRAY['swift']),
    ('sql',            'SQL',             'LANGUAGE',  ARRAY['sql']),
    ('spring',         'Spring',          'FRAMEWORK', ARRAY['spring', 'spring framework']),
    ('spring-boot',    'Spring Boot',     'FRAMEWORK', ARRAY['spring boot', 'springboot']),
    ('jpa',            'JPA / Hibernate', 'FRAMEWORK', ARRAY['jpa', 'hibernate']),
    ('node',           'Node.js',         'FRAMEWORK', ARRAY['node', 'node.js', 'nodejs']),
    ('nestjs',         'NestJS',          'FRAMEWORK', ARRAY['nestjs', 'nest.js']),
    ('django',         'Django',          'FRAMEWORK', ARRAY['django']),
    ('fastapi',        'FastAPI',         'FRAMEWORK', ARRAY['fastapi']),
    ('flask',          'Flask',           'FRAMEWORK', ARRAY['flask']),
    ('react',          'React',           'FRAMEWORK', ARRAY['react', 'react.js', 'reactjs']),
    ('nextjs',         'Next.js',         'FRAMEWORK', ARRAY['next.js', 'nextjs']),
    ('vue',            'Vue',             'FRAMEWORK', ARRAY['vue', 'vue.js', 'vuejs']),
    ('grpc',           'gRPC',            'FRAMEWORK', ARRAY['grpc']),
    ('graphql',        'GraphQL',         'FRAMEWORK', ARRAY['graphql']),
    ('aws',            'AWS',             'CLOUD',     ARRAY['aws', 'amazon web services']),
    ('gcp',            'Google Cloud',    'CLOUD',     ARRAY['gcp', 'google cloud']),
    ('azure',          'Azure',           'CLOUD',     ARRAY['azure']),
    ('ncloud',         'Naver Cloud',     'CLOUD',     ARRAY['ncloud', 'naver cloud']),
    ('postgresql',     'PostgreSQL',      'DATABASE',  ARRAY['postgresql', 'postgres']),
    ('mysql',          'MySQL',           'DATABASE',  ARRAY['mysql', 'mariadb']),
    ('oracle-db',      'Oracle Database', 'DATABASE',  ARRAY['oracle db', 'oracle database']),
    ('mongodb',        'MongoDB',         'DATABASE',  ARRAY['mongodb', 'mongo']),
    ('redis',          'Redis',           'DATABASE',  ARRAY['redis']),
    ('elasticsearch',  'Elasticsearch',   'DATABASE',  ARRAY['elasticsearch', 'opensearch']),
    ('timescaledb',    'TimescaleDB',     'DATABASE',  ARRAY['timescaledb', 'timescale']),
    ('influxdb',       'InfluxDB',        'DATABASE',  ARRAY['influxdb', 'influx']),
    ('clickhouse',     'ClickHouse',      'DATABASE',  ARRAY['clickhouse']),
    ('docker',         'Docker',          'INFRA',     ARRAY['docker', 'container']),
    ('kubernetes',     'Kubernetes',      'INFRA',     ARRAY['kubernetes', 'k8s', 'eks', 'gke']),
    ('terraform',      'Terraform',       'INFRA',     ARRAY['terraform']),
    ('linux',          'Linux',           'INFRA',     ARRAY['linux', 'unix']),
    ('nginx',          'Nginx',           'INFRA',     ARRAY['nginx']),
    ('kafka',          'Kafka',           'INFRA',     ARRAY['kafka']),
    ('rabbitmq',       'RabbitMQ',        'INFRA',     ARRAY['rabbitmq']),
    ('airflow',        'Airflow',         'INFRA',     ARRAY['airflow']),
    ('spark',          'Spark',           'INFRA',     ARRAY['spark', 'apache spark']),
    ('prometheus',     'Prometheus',      'TOOL',      ARRAY['prometheus']),
    ('grafana',        'Grafana',         'TOOL',      ARRAY['grafana']),
    ('git',            'Git',             'TOOL',      ARRAY['git', 'github', 'gitlab']),
    ('jenkins',        'Jenkins',         'TOOL',      ARRAY['jenkins']),
    ('github-actions', 'GitHub Actions',  'TOOL',      ARRAY['github actions']),
    ('argocd',         'Argo CD',         'TOOL',      ARRAY['argocd', 'argo cd']),
    ('ci-cd',          'CI/CD',           'PRACTICE',  ARRAY['ci/cd', 'cicd', 'continuous integration', 'continuous delivery']),
    ('testing',        'Automated testing', 'PRACTICE', ARRAY['junit', 'pytest', 'unit test', 'integration test', 'tdd']),
    ('observability',  'Observability',   'PRACTICE',  ARRAY['observability', 'monitoring', 'tracing', 'opentelemetry']),
    ('microservices',  'Microservices',   'PRACTICE',  ARRAY['microservice', 'microservices', 'msa']),
    ('distributed-systems', 'Distributed systems', 'PRACTICE', ARRAY['distributed system', 'distributed systems']),
    ('api-design',     'API design',      'PRACTICE',  ARRAY['rest api', 'restful', 'api design', 'openapi']),
    ('data-pipeline',  'Data pipelines',  'PRACTICE',  ARRAY['etl', 'elt', 'data pipeline', 'data engineering']),
    ('machine-learning', 'Machine learning', 'DOMAIN', ARRAY['machine learning', 'ml', 'deep learning']),
    ('llm',            'LLM',             'DOMAIN',    ARRAY['llm', 'large language model', 'rag'])
ON CONFLICT (slug) DO NOTHING;

INSERT INTO candidate_profiles (code, display_name, profile) VALUES
    ('default', 'Default candidate profile', '{
        "education": {
            "institution": "UC San Diego",
            "program": "Mathematics-Computer Science",
            "level": "BACHELOR"
        },
        "target": {
            "seniority_buckets": ["A", "B", "C"],
            "role_families": ["BACKEND", "PLATFORM", "DATA_INFRASTRUCTURE", "FULLSTACK"],
            "max_years_experience": 2
        },
        "skills": {
            "strong": ["java", "python", "typescript", "postgresql", "redis", "docker"],
            "working": ["spring-boot", "prometheus", "grafana", "timescaledb", "influxdb", "ci-cd", "testing"],
            "interest": ["kubernetes", "distributed-systems", "observability", "data-pipeline", "api-design"]
        },
        "preferences": {
            "location_countries": ["KR"],
            "remote_policies": ["ONSITE", "HYBRID", "REMOTE"],
            "accepts_startup": true
        }
    }'::jsonb)
ON CONFLICT (code) DO NOTHING;
