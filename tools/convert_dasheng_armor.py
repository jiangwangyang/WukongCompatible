#!/usr/bin/env python3
# 将 GeckoLib 盔甲模型 (dasheng.geo.json, 原版 128 基准) 转换为 EpicFight 20.14.17 盔甲网格 JSON
# 输出: src/main/resources/assets/wukong/animmodels/armor/dasheng_{h,c,l,f}.json
#
# 与旧版的区别 (标定):
# 1. 坐标不做平移: EF 空间即原版模型空间 (px/16, 脚底 0), glb 坐标天然对齐 (详见 DELTA_Y 注释).
# 2. 关节绑定照抄官方混合曲线 (逐顶点验证自官方 JSON):
#    头盔 -> Head(9);
#    躯干 -> Torso(7)/Chest(8) 在 y=0.85~1.40 线性过渡 (官方 chestplate torso);
#    手臂 -> y<1.125 为 Hand(12/17), y>1.125 为 Arm(11/16) (官方 chestplate 在 1.125 硬切);
#    腿/靴 -> y<=0.365 为 Leg(2/5), 0.365<y<=0.735 为 Knee(3/6), 之上为 Thigh(1/4) (官方 leggins/boots).
# 3. UV 按原版 geo 的 128 基准归一化 (与 256x256 贴图自洽).
#
# 几何换算移植自 geckolib-4.8.3:
#   BakedModelFactory$Builtin.constructCube / VertexSet (方块角点/膨胀/镜像/UV)
#   RenderUtils.translateToPivotPoint / rotateMatrixAroundCube / translateAwayFromPivotPoint (方块旋转, 枢轴 -x,y,z 且 /16, 角度 -rx,-ry,+rz)
# 输出坐标序 = EpicFight JSON 空间 (x, z, y), y 轴向上, 脚底为 0
import json
import math
import os

GEO_PATH = "src/main/resources/assets/wukong/geo/item/armor/dasheng.geo.json"
OUT_DIR = "src/main/resources/assets/wukong/animmodels/armor"

# 每骨骼垂直标定 (块).EF 空间 = 原版模型空间 (px/16, 脚底 0), glb 坐标天然对齐, 全部为 0.
# 依据: 官方头盔顶 2.067 约等于 33px/16 (原版头顶 32px + 1px 膨胀); 本模型头盔冠底 glb 1.99 正好落在 EF 头顶 2.0 上.
# 若游戏内需要微调, 每 0.0625 约合 1 像素.
DELTA_Y = {
    "armorHead": 0.0,
    "armorBody": 0.0,
    "armorRightArm": 0.0,
    "armorLeftArm": 0.0,
    "armorRightLeg": 0.0,
    "armorLeftLeg": 0.0,
    "armorRightBoot": 0.0,
    "armorLeftBoot": 0.0,
}

# EpicFight biped 关节索引 (assets/epicfight/animmodels/entity/biped.json 的 joints 顺序)
JOINT = {
    "Root": 0, "Thigh_R": 1, "Leg_R": 2, "Knee_R": 3, "Thigh_L": 4, "Leg_L": 5,
    "Knee_L": 6, "Torso": 7, "Chest": 8, "Head": 9, "Shoulder_R": 10, "Arm_R": 11,
    "Hand_R": 12, "Tool_R": 13, "Elbow_R": 14, "Shoulder_L": 15, "Arm_L": 16,
    "Hand_L": 17, "Tool_L": 18, "Elbow_L": 19,
}

ARM_SPLIT_Y = 1.125   # 官方 chestplate 手臂 Arm/Hand 硬切高度 (18px)
LEG_KNEE_Y = 0.365    # 官方 leggins/boots Leg/Knee 分界
LEG_THIGH_Y = 0.735   # 官方 leggins/boots Knee/Thigh 分界
TORSO_LO = 0.85       # 官方 chestplate torso Torso 满权下限
TORSO_HI = 1.40       # 官方 chestplate torso Chest 满权上限

# geckolib 顶点表 (VertexSet): 键为角点, 元组 = (是否取 x 最大侧, 是否取 y 最大侧, 是否取 z 最大侧)
# 依据 BakedModelFactory$VertexSet 构造器: bottomLeftFront=(x_max,y_min,z_min), bottomRightFront=(x_max,y_min,z_max)
# (旧表 blf/brf 两角写反, 导致 east/north/south/down 四个面为蝶形扭曲四边形, UV 角落错配)
V = {
    "blb": (0, 0, 0), "brb": (0, 0, 1), "tlb": (0, 1, 0), "trb": (0, 1, 1),
    "tlf": (1, 1, 0), "trf": (1, 1, 1), "blf": (1, 0, 0), "brf": (1, 0, 1),
}
QUADS = {
    "west":  (["trb", "tlb", "blb", "brb"], (-1.0, 0.0, 0.0)),
    "east":  (["tlf", "trf", "brf", "blf"], (1.0, 0.0, 0.0)),
    "north": (["tlb", "tlf", "blf", "blb"], (0.0, 0.0, -1.0)),
    "south": (["trf", "trb", "brb", "brf"], (0.0, 0.0, 1.0)),
    "up":    (["trb", "trf", "tlf", "tlb"], (0.0, 1.0, 0.0)),
    "down":  (["blb", "blf", "brf", "brb"], (0.0, -1.0, 0.0)),
}
FACE_ORDER = ["west", "east", "north", "south", "up", "down"]


def rot_matrix(rx, ry, rz):
    # 移植 RenderUtils.rotateMatrixAroundCube: 三个 rotationXYZ 四元数依次右乘, 合成 R = Rz(rz)*Ry(ry)*Rx(rx), 角度弧度
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    def mm(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mm(mz, mm(my, mx))


def apply(m, v):
    return (m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2])


def joints_for(bone_name, y):
    # 官方混合曲线, 返回 [(关节索引, 权重), ...]
    if bone_name == "armorHead":
        return [(JOINT["Head"], 1.0)]
    if bone_name == "armorBody":
        t = max(0.0, min(1.0, (TORSO_HI - y) / (TORSO_HI - TORSO_LO)))
        return [(JOINT["Torso"], round(t, 4)), (JOINT["Chest"], round(1.0 - t, 4))]
    if bone_name == "armorRightArm":
        return [(JOINT["Hand_R"], 1.0)] if y < ARM_SPLIT_Y else [(JOINT["Arm_R"], 1.0)]
    if bone_name == "armorLeftArm":
        return [(JOINT["Hand_L"], 1.0)] if y < ARM_SPLIT_Y else [(JOINT["Arm_L"], 1.0)]
    if bone_name in ("armorRightLeg", "armorRightBoot"):
        if y <= LEG_KNEE_Y:
            return [(JOINT["Leg_R"], 1.0)]
        if y <= LEG_THIGH_Y:
            return [(JOINT["Knee_R"], 1.0)]
        return [(JOINT["Thigh_R"], 1.0)]
    if bone_name in ("armorLeftLeg", "armorLeftBoot"):
        if y <= LEG_KNEE_Y:
            return [(JOINT["Leg_L"], 1.0)]
        if y <= LEG_THIGH_Y:
            return [(JOINT["Knee_L"], 1.0)]
        return [(JOINT["Thigh_L"], 1.0)]
    raise SystemExit("未知骨骼 %s" % bone_name)


# 每件盔甲输出: 文件名 -> [(部件名, 骨骼名), ...]  部件名与 EpicFight 官方网格一致
PIECES = {
    "dasheng_h": [("head", "armorHead")],
    "dasheng_c": [("torso", "armorBody"), ("leftArm", "armorLeftArm"), ("rightArm", "armorRightArm")],
    "dasheng_l": [("noGroups", "armorLeftLeg"), ("noGroups", "armorRightLeg")],
    "dasheng_f": [("noGroups", "armorLeftBoot"), ("noGroups", "armorRightBoot")],
}


def main():
    # 主流程: 读取 GeckoLib geo 模型, 逐件盔甲生成顶点/UV/法线/蒙皮权重数据, 写出 EpicFight 网格 JSON
    with open(GEO_PATH, encoding="utf-8") as f:
        geo = json.load(f)["minecraft:geometry"][0]
    tex_w = float(geo["description"]["texture_width"])
    tex_h = float(geo["description"]["texture_height"])
    bones = {b["name"]: b for b in geo["bones"]}

    os.makedirs(OUT_DIR, exist_ok=True)
    for file_name, parts in PIECES.items():
        positions, normals, uvs = [], [], []
        pos_index, norm_index, uv_index = {}, {}, {}
        vcounts, weights, vindices = [], [], []
        weight_index = {}
        out_parts = {}

        def add_position(p):
            key = (round(p[0], 6), round(p[1], 6), round(p[2], 6))
            if key not in pos_index:
                pos_index[key] = len(positions)
                positions.append(key)
            return pos_index[key]

        def add_normal(n):
            ln = math.sqrt(n[0] ** 2 + n[1] ** 2 + n[2] ** 2) or 1.0
            key = (round(n[0] / ln, 5), round(n[1] / ln, 5), round(n[2] / ln, 5))
            if key not in norm_index:
                norm_index[key] = len(normals)
                normals.append(key)
            return norm_index[key]

        def add_uv(t):
            key = (round(t[0], 6), round(t[1], 6))
            if key not in uv_index:
                uv_index[key] = len(uvs)
                uvs.append(key)
            return uv_index[key]

        def add_weight(w):
            key = round(w, 4)
            if key not in weight_index:
                weight_index[key] = len(weights)
                weights.append(key)
            return weight_index[key]

        skinned = {}  # 顶点位置索引 -> [(关节, 权重索引), ...]

        def add_skinned_vertex(p_glb, bone_name):
            # EpicFight JSON 为 Blender 风格坐标系, 加载时施加 Rx(-90):
            # 存储 (a,b,c) -> MC (a, c, -b), 故正确存储 = (mc_x, -mc_z, mc_y)
            y_cal = p_glb[1] + DELTA_Y[bone_name]
            idx = add_position((p_glb[0], -p_glb[2], y_cal))
            if idx not in skinned:
                skinned[idx] = [(j, add_weight(w)) for j, w in joints_for(bone_name, y_cal)]
            return idx

        for part_name, bone_name in parts:
            bone = bones[bone_name]
            if bone.get("rotation"):
                raise SystemExit("骨骼 %s 存在整体旋转, 脚本未处理" % bone_name)
            part_arr = out_parts.setdefault(part_name, [])
            for cube in bone.get("cubes", []):
                uv_faces = cube.get("uv")
                if not isinstance(uv_faces, dict):
                    raise SystemExit("骨骼 %s 存在 box uv 方块, 脚本未处理" % bone_name)
                ox, oy, oz = cube["origin"]
                sx, sy, sz = cube["size"]
                inflate = cube.get("inflate", bone.get("inflate", 0.0)) / 16.0
                mirror = bool(cube.get("mirror"))
                # constructCube: 内部空间(块)原点与尺寸, x 取负 (bedrock -> geckolib 内部空间)
                bx, by, bz = -(ox + sx) / 16.0, oy / 16.0, oz / 16.0
                size = (sx / 16.0, sy / 16.0, sz / 16.0)
                corners = {}
                for name, (ix, iy, iz) in V.items():
                    corners[name] = (
                        bx + (size[0] if ix else 0.0) + (inflate if ix else -inflate),
                        by + (size[1] if iy else 0.0) + (inflate if iy else -inflate),
                        bz + (size[2] if iz else 0.0) + (inflate if iz else -inflate),
                    )
                # 方块旋转: geckolib 为 (-rx, -ry, rz) 角度制, 合成 R=Rz*Ry*Rx, 绕 pivot 旋转, pivot 同样 x 取负并 /16
                crx, cry, crz = cube.get("rotation", [0.0, 0.0, 0.0])
                cpx, cpy, cpz = cube.get("pivot", [ox + sx / 2.0, oy + sy / 2.0, oz + sz / 2.0])
                rot = rot_matrix(math.radians(-crx), math.radians(-cry), math.radians(crz))
                pivot = (-cpx / 16.0, cpy / 16.0, cpz / 16.0)

                def transform(p):
                    q = (p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2])
                    r = apply(rot, q)
                    return (r[0] + pivot[0], r[1] + pivot[1], r[2] + pivot[2])

                for face in FACE_ORDER:
                    face_uv = uv_faces.get(face)
                    if face_uv is None:
                        continue
                    vert_names, face_normal = QUADS[face]
                    fu, fv = face_uv["uv"]
                    fsu, fsv = face_uv["uv_size"]
                    # GeoQuad.build: 非镜像时交换 u0/u1, uv 按贴图基准归一化
                    u0, u1 = (fu + fsu) / tex_w, fu / tex_w
                    if mirror:
                        u0, u1 = u1, u0
                    v0, v1 = fv / tex_h, (fv + fsv) / tex_h
                    quad_uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
                    n_glb = apply(rot, face_normal)
                    ni = add_normal((n_glb[0], -n_glb[2], n_glb[1]))
                    quad = []
                    for i, vn in enumerate(vert_names):
                        p_glb = transform(corners[vn])
                        pi = add_skinned_vertex(p_glb, bone_name)
                        ui = add_uv(quad_uv[i])
                        quad.append([pi, ui, ni])
                    # 渲染模式为 TRIANGLES, parts 数组必须预三角化: 每个四边形输出 (0,1,2) 与 (0,2,3) 两个三角形
                    # 依据: Mesh$DrawingFunction.NEW_ENTITY 逐顶点写缓冲 + makeTriangulated 将模式改为 TRIANGLES;
                    # 官方网格 head/hat 部件 30 顶点 = 10 三角形 (原版头盔 12 面去掉 2 个底面) 佐证.
                    part_arr.extend(quad[0])
                    part_arr.extend(quad[1])
                    part_arr.extend(quad[2])
                    part_arr.extend(quad[0])
                    part_arr.extend(quad[2])
                    part_arr.extend(quad[3])

        for idx in range(len(positions)):
            influences = skinned[idx]
            vcounts.append(len(influences))
            for joint_id, wi in influences:
                vindices.extend([joint_id, wi])

        mesh = {
            "vertices": {
                "positions": {"stride": 3, "count": len(positions),
                              "array": [c for p in positions for c in p]},
                "uvs": {"stride": 2, "count": len(uvs),
                        "array": [c for t in uvs for c in t]},
                "normals": {"stride": 3, "count": len(normals),
                            "array": [c for n in normals for c in n]},
                "vcounts": {"stride": 1, "count": len(vcounts), "array": vcounts},
                "weights": {"stride": 1, "count": len(weights), "array": weights},
                "vindices": {"stride": 1, "count": len(vindices), "array": vindices},
                "parts": {name: {"stride": 3, "count": len(arr) // 3, "array": arr}
                          for name, arr in out_parts.items()},
            }
        }
        out_path = os.path.join(OUT_DIR, file_name + ".json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(mesh, f, indent=1)
        print("%s: 顶点 %d, uv %d, 法线 %d, 面 %d -> %s" %
              (file_name, len(positions), len(uvs), len(normals),
               sum(len(a) // 12 for a in out_parts.values()), out_path))


if __name__ == "__main__":
    main()
