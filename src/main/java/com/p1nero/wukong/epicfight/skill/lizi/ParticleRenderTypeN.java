package com.p1nero.wukong.epicfight.skill.lizi;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

// 粒子渲染类型扩展: 提供带光照且不剔除背面(关闭背面剔除)的粒子渲染方式
public interface ParticleRenderTypeN {
    // 受光且关闭背面剔除的粒子渲染类型, 用于特殊粒子贴图绘制
    ParticleRenderType PARTICLE_SHEET_LIT_NO_CULL =
            new ParticleRenderType() {
                // 渲染开始: 启用深度写入与alpha混合, 关闭背面剔除, 绑定粒子着色器与粒子图集, 并以QUADS模式开启缓冲
                public void begin(BufferBuilder p_107462_, TextureManager p_107463_) {
                    RenderSystem.depthMask(true);
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(
                            GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    RenderSystem.disableCull();
                    RenderSystem.setShader(GameRenderer::getParticleShader);
                    // 弃用API迁移: TextureAtlas.LOCATION_PARTICLES已被原版标记废弃, 1.20.1无替代常量,
                    // 按其常量值(反编译核实为minecraft:textures/atlas/particles.png)等价内联
                    RenderSystem.setShaderTexture(
                            0,
                            ResourceLocation.withDefaultNamespace("textures/atlas/particles.png"));
                    p_107462_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                // 渲染结束: 提交缓冲并完成绘制
                public void end(Tesselator p_107465_) {
                    p_107465_.end();
                }

                // 返回该渲染类型的名称
                public String toString() {
                    return "PARTICLE_SHEET_LIT_NO_CULL";
                }
            };
}
