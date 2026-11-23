CREATE TABLE skills (
                        id         BIGINT      NOT NULL AUTO_INCREMENT,
                        name       VARCHAR(50) NOT NULL,                 -- normalized: lowercase, single spaces
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_skills_name (name)                 -- duplicates impossible + prefix search (LIKE 'rea%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_skills (
                             user_id    BIGINT      NOT NULL,
                             skill_id   BIGINT      NOT NULL,
                             kind       VARCHAR(10) NOT NULL,                 -- 'SKILL' or 'INTEREST'
                             created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                             PRIMARY KEY (user_id, kind, skill_id),           -- "skills of this user" + no duplicates
                             KEY idx_user_skills_skill (skill_id, kind, user_id),   -- reverse: "who has React?" (used by search later)
                             CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES users (id),
                             CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skills (id),
                             CONSTRAINT chk_us_kind CHECK (kind IN ('SKILL','INTEREST'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed vocabulary (~130). Add more in a new migration (V5...), never edit this file after merge.
INSERT INTO skills (name) VALUES
                              ('java'),('python'),('c'),('c++'),('c#'),('javascript'),('typescript'),('go'),('rust'),('kotlin'),
                              ('swift'),('dart'),('php'),('ruby'),('r'),('matlab'),('sql'),('bash'),('scala'),('haskell'),
                              ('html'),('css'),('react'),('react native'),('angular'),('vue'),('next.js'),('node.js'),('express'),
                              ('tailwind css'),('bootstrap'),('redux'),('graphql'),('rest api'),('webpack'),('svelte'),
                              ('spring boot'),('spring'),('hibernate'),('jpa'),('maven'),('gradle'),('junit'),('mockito'),
                              ('microservices'),('django'),('flask'),('fastapi'),('laravel'),('.net'),('asp.net'),
                              ('android'),('ios'),('flutter'),('jetpack compose'),
                              ('mysql'),('postgresql'),('mongodb'),('redis'),('firebase'),('sqlite'),('oracle'),
                              ('elasticsearch'),('kafka'),('rabbitmq'),
                              ('git'),('github'),('github actions'),('docker'),('kubernetes'),('linux'),('aws'),('azure'),('gcp'),
                              ('jenkins'),('nginx'),('terraform'),
                              ('data structures'),('algorithms'),('competitive programming'),('system design'),('oop'),
                              ('operating systems'),('computer networks'),('dbms'),('design patterns'),
                              ('machine learning'),('deep learning'),('nlp'),('computer vision'),('data science'),('data analysis'),
                              ('pandas'),('numpy'),('scikit-learn'),('tensorflow'),('pytorch'),('opencv'),('power bi'),('tableau'),
                              ('excel'),('statistics'),('generative ai'),('llm'),
                              ('cybersecurity'),('ethical hacking'),('blockchain'),('solidity'),('iot'),('arduino'),
                              ('raspberry pi'),('embedded systems'),('robotics'),('vlsi'),
                              ('ui design'),('ux design'),('figma'),('graphic design'),('video editing'),('photography'),
                              ('content writing'),('technical writing'),('public speaking'),('product management'),
                              ('entrepreneurship'),('game development'),('unity'),('unreal engine'),
                              ('augmented reality'),('virtual reality'),('open source'),('testing'),('selenium'),('postman'),
                              ('agile'),('leadership');