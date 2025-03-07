INSERT INTO roles (name)
VALUES 
('ROLE_USER'),
('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;


-- 插入 polls 表的数据
WITH inserted_polls AS (
    INSERT INTO polls (question, expiration_date_time, created_by, updated_by)
    VALUES 
        ('谁是中国历史上第一个皇帝？', '2025-12-31 23:59:59', 1, 1),
        ('美国独立战争开始的标志是什么？', '2025-12-31 23:59:59', 1, 1),
        ('工业革命最早起源于哪个国家？', '2025-12-31 23:59:59', 1, 1),
        ('三国时期“三英战吕布”中的“三英”不包括以下谁？', '2025-12-31 23:59:59', 1, 1),
        ('法国大革命开始的标志是？', '2025-12-31 23:59:59', 1, 1),
        ('日本明治维新开始于哪一年？', '2025-12-31 23:59:59', 1, 1),
        ('第一次世界大战爆发的导火线是什么？', '2025-12-31 23:59:59', 1, 1),
        ('唐朝由盛转衰的标志是？', '2025-12-31 23:59:59', 1, 1),
        ('英国资产阶级革命开始的标志是？', '2025-12-31 23:59:59', 1, 1),
        ('古代埃及人主要使用的文字是？', '2025-12-31 23:59:59', 1, 1),
        ('亚历山大大帝是哪个国家的君主？', '2025-12-31 23:59:59', 1, 1),
        ('中国历史上的“开元盛世”是哪个皇帝统治时期？', '2025-12-31 23:59:59', 1, 1),
        ('新航路开辟中，发现美洲新大陆的航海家是？', '2025-12-31 23:59:59', 1, 1),
        ('罗马帝国的第一位皇帝是谁？', '2025-12-31 23:59:59', 1, 1),
        ('印度种姓制度中，地位最高的是？', '2025-12-31 23:59:59', 1, 1),
        ('中国历史上的“贞观之治”是哪个皇帝统治时期？', '2025-12-31 23:59:59', 1, 1),
        ('第二次世界大战全面爆发的标志是？', '2025-12-31 23:59:59', 1, 1),
        ('蒙古帝国的创始人是谁？', '2025-12-31 23:59:59', 1, 1),
        ('古希腊著名的哲学家苏格拉底是被什么罪名处死的？', '2025-12-31 23:59:59', 1, 1),
        ('中国历史上的“康乾盛世”不包括以下哪个皇帝？', '2025-12-31 23:59:59', 1, 1)
    RETURNING id
)
-- 插入 choices 表的数据
INSERT INTO choices (text, poll_id)
VALUES 
    -- 问题 1 的选项
    ('秦始皇', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 0)),
    ('汉武帝', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 0)),
    ('唐太宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 0)),
    ('宋太祖', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 0)),
    -- 问题 2 的选项
    ('莱克星顿的枪声', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 1)),
    ('波士顿倾茶事件', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 1)),
    ('萨拉托加大捷', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 1)),
    ('约克镇战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 1)),
    -- 问题 3 的选项
    ('英国', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 2)),
    ('美国', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 2)),
    ('法国', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 2)),
    ('德国', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 2)),
    -- 问题 4 的选项
    ('刘备', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 3)),
    ('关羽', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 3)),
    ('张飞', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 3)),
    ('赵云', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 3)),
    -- 问题 5 的选项
    ('攻占巴士底狱', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 4)),
    ('《人权宣言》的发表', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 4)),
    ('雅各宾派专政', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 4)),
    ('热月政变', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 4)),
    -- 问题 6 的选项
    ('1868 年', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 5)),
    ('1853 年', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 5)),
    ('1871 年', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 5)),
    ('1894 年', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 5)),
    -- 问题 7 的选项
    ('萨拉热窝事件', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 6)),
    ('马恩河战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 6)),
    ('凡尔登战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 6)),
    ('索姆河战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 6)),
    -- 问题 8 的选项
    ('安史之乱', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 7)),
    ('黄巢起义', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 7)),
    ('玄武门之变', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 7)),
    ('陈桥兵变', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 7)),
    -- 问题 9 的选项
    ('长期议会的召开', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 8)),
    ('纳西比战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 8)),
    ('光荣革命', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 8)),
    ('《权利法案》的颁布', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 8)),
    -- 问题 10 的选项
    ('象形文字', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 9)),
    ('楔形文字', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 9)),
    ('字母文字', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 9)),
    ('甲骨文', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 9)),
    -- 问题 11 的选项
    ('马其顿', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 10)),
    ('希腊', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 10)),
    ('罗马', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 10)),
    ('波斯', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 10)),
    -- 问题 12 的选项
    ('唐玄宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 11)),
    ('唐太宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 11)),
    ('唐高宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 11)),
    ('武则天', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 11)),
    -- 问题 13 的选项
    ('哥伦布', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 12)),
    ('达伽马', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 12)),
    ('麦哲伦', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 12)),
    ('迪亚士', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 12)),
    -- 问题 14 的选项
    ('屋大维', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 13)),
    ('恺撒', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 13)),
    ('庞培', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 13)),
    ('克拉苏', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 13)),
    -- 问题 15 的选项
    ('婆罗门', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 14)),
    ('刹帝利', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 14)),
    ('吠舍', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 14)),
    ('首陀罗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 14)),
    -- 问题 16 的选项
    ('唐太宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 15)),
    ('唐玄宗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 15)),
    ('唐高祖', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 15)),
    ('武则天', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 15)),
    -- 问题 17 的选项
    ('德国入侵波兰', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 16)),
    ('日本偷袭珍珠港', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 16)),
    ('诺曼底登陆', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 16)),
    ('斯大林格勒战役', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 16)),
    -- 问题 18 的选项
    ('成吉思汗', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 17)),
    ('忽必烈', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 17)),
    ('窝阔台', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 17)),
    ('拖雷', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 17)),
    -- 问题 19 的选项
    ('腐蚀青年', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 18)),
    ('叛国罪', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 18)),
    ('谋杀罪', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 18)),
    ('抢劫罪', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 18)),
    -- 问题 20 的选项
    ('雍正帝', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 19)),
    ('康熙帝', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 19)),
    ('乾隆帝', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 19)),
    ('嘉庆帝', (SELECT id FROM inserted_polls LIMIT 1 OFFSET 19));

