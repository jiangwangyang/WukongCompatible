package com.p1nero.wukong.epicfight.skill.custom.wukong;

import yesman.epicfight.skill.SkillBuilder;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.entity.FakeWukongEntityPatch;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.FakeWukongEntityRegistry;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import com.p1nero.wukong.epicfight.compat.EpicFightDamageType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

import static yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.STAMINA;

/**
 * 鎴虫閲嶅嚮
 */

public class ThrustHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a02b-0242ac114515");
    @NotNull
    protected final StaticAnimationProvider[] animations;//0~4鍏辨湁浜旂閲嶅嚮
    @NotNull
    protected StaticAnimationProvider xuli_start;
    protected StaticAnimationProvider stepinch;
    protected StaticAnimationProvider footage;
    protected StaticAnimationProvider fengchuanhua;
    protected StaticAnimationProvider chargePre;
    protected StaticAnimationProvider juesick_start;
    protected StaticAnimationProvider juesick_loop;
    protected StaticAnimationProvider juesick_end;
    protected StaticAnimationProvider jumpAttackHeavy;



    @Override
    public  List<StaticAnimationProvider> getHeavyAttacks(){
        List<StaticAnimationProvider> staticAnimations =new java.util.ArrayList<>(List.of(animations));
        staticAnimations.add(footage);
        staticAnimations.add(fengchuanhua);
        return staticAnimations;
    }


    public static Builder createChargedAttack(){
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    public ThrustHeavyAttack(Builder builder) {
        super(builder);
        chargePre = builder.pre;
        xuli_start  = builder.start;
        this.animations = builder.animationProviders;

        stepinch = builder.stepinch;
        fengchuanhua = builder.fengchuanhua;
        footage = builder.footage;
        juesick_start = builder.juesick_start;
        juesick_loop = builder.juesick_loop;
        juesick_end = builder.juesick_end;

        jumpAttackHeavy = builder.jumpAttackHeavy;
    }

    /**
     * 鍦ㄨ鏃跺懆鏈熷唴浣跨敤鎶€鑳芥墠绠椾娇鐢ㄨ鐢燂紝鍚﹀垯瑙嗕负閲嶅嚮
     * 闀挎寜寰幆绗竴娈佃鐢熺殑鍒ゆ柇鍦▄@link ThrustHeavyAttack#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();

        dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack(), player);//0鏄熶篃鏄槦锛?       // dataManager.setDataSync(WukongSkillDataKeys.Thrust_CAN_SECOND_DERIVE.get(),dataManager.getDataValue(WukongSkillDataKeys.Thrust_STEOP_BACK.get()) , player);//绗簩娈垫淳鐢熻В閿?       // WukongMoveset.LOGGER.info("閲嶅嚮 瀵搁€€鍊掕鏃?{}",+dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get()) );
//        if(dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_TIMER.get()) > 0 ) {
//            this.setStackSynchronize(container, container.getStack()-container.getStack());
//          if (container.getStack()==4){executer.playAnimationSynchronized(fengchuanhua.get(), 0F);}
//          if (container.getStack()!=4 && container.getStack()!=0){executer.playAnimationSynchronized(animations[container.getStack()].get(), 0F);}
//            dataManager.setData(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), 0);
//
//        }else if(container.getStack()==4 ) {
//            this.setStackSynchronize(container, container.getStack()-container.getStack());
//            dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack(), player);
//            dataManager.setData(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 0);
//            executer.playAnimationSynchronized(fengchuanhua.get(), 0F);
//            executer.playSound(WuKongSounds.PERFECT_FENGCHUANHUA.get(), 1, 1);
//        } else


        if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get()) > 0) {//鏅€氭敾鍑昏В閿佸閫€鎶€鑳?
        dataManager.setData(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 0);
            executer.playAnimationSynchronized(stepinch.get(), 0F);
        }else if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get())> 0) {
            dataManager.setDataSync(WukongSkillDataKeys.THRUST_METERS_BACK.get(), true, player);
        }else{
            executer.playAnimationSynchronized(xuli_start.get(), 0F);
        }
        super.executeOnServer(container, args);
    }

    public void wuKongFengShenZhiLl(ServerPlayer player, AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        //wuKongFengShenZhiLl(serverPlayer, animations[container.getStack()].get());
        List<Integer> summonedIds = FakeWukongEntityRegistry.getFakeWukongEntityIds(player);
        for (Integer id : summonedIds) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            Entity entity = serverLevel.getEntity(id);
            if (entity == null) {
                FakeWukongEntityRegistry.clearFakeWukongEntityIdsIfNotExist(player, id);
                continue;
            }
            FakeWukongEntityPatch patch = EpicFightCapabilities.getEntityPatch(entity, FakeWukongEntityPatch.class);
            if (patch != null) {
                patch.playAnimationSynchronized(animation, 0);
            }
        }
    }

    @Override
    public void onInitiate(SkillContainer container) {
        SkillDataManager dataManager = container.getDataManager();
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
            ServerPlayer player = serverPlayerPatch.getOriginal();
           // SkillContainer container = serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE);
            if(event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(WukongAnimations.THRUST_JUESICK_LOOP.get())){
                if (container.getStack()<4){
                    container.getSkill().setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue()*3);
                }
            }
            if (event.getAttackDamage()>0.0){
                modifyStamina(event.getPlayerPatch().getOriginal(), 2.0F);
                if (event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(WukongAnimations.PILLAR_HEAVY_FENGYUNZHUAN.get())) {
                    createRepelForAttackTarget(player, event.getForgeEvent().getEntity(), 2);
                }else if (event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(WukongAnimations.THRUST_FOOTAGE.get())) {
                    createRepelForAttackTarget(player, event.getForgeEvent().getEntity(), 1);
                }else if (event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(WukongAnimations.THRUST_CHARGED3.get())) {
                    createRepelForAttackTarget(player, event.getForgeEvent().getEntity(), 1.5);
                }
            }
            //  WukongMoveset.LOGGER.info("閲嶅嚮 瀵搁€€鍊掕鏃?{}",+dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get()) );
        }));


        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID, (event -> {
                    ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
                    ServerPlayer player = serverPlayerPatch.getOriginal();
                    CapabilityItem capabilityItem = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
                    if(!WukongWeaponCategories.isWeaponValid(event.getPlayerPatch())){
                        return;
                    }
                    var autoAnimations = capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                    for(int i = 0; i < autoAnimations.size(); i++){
                        if(autoAnimations.get(i).get().equals(event.getAnimation()) && i < 6){

                            container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 30, player);
                            container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_CAN_SECOND_DERIVE.get(), false, player);
                            container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_STEOP_BACK.get(), false, player);
                            dataManager.setDataSync(WukongSkillDataKeys.THRUST_METERS_BACK.get(), false, player);
                          //  container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_DERIVE_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue(), player);
                            container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_DERIVE_TIMER_TWO.get(), 0, player);
                            return;
                        }
                    }
                }));

        //鐩戝惉鐜╁閫€瀵稿彈鍒颁激瀹?
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            if (event.getDamageSource().is(DamageTypes.FALL) && container.getDataManager().getDataValue(WukongSkillDataKeys.THRUST_PROTECT_NEXT_FALL.get())) {
                event.setCanceled(true);
                event.setCanceled(true);
                event.setResult(AttackResult.ResultType.MISSED);
                event.getPlayerPatch().getOriginal().resetFallDistance();
                container.getDataManager().setData(WukongSkillDataKeys.THRUST_PROTECT_NEXT_FALL.get(), false);
            }
            if(container.getDataManager().getDataValue(WukongSkillDataKeys.Thrust_STEOP_BACK.get())){
              //  if(event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(stepinch.get())){
                container.getSkill().setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue() * 90); // 鑾峰緱澶ч噺妫嶅娍
                PacketRelay.sendToAll(PacketHandler.INSTANCE, new AddEntityAfterImageParticle(event.getPlayerPatch().getOriginal().getId()));
                event.getPlayerPatch().playSound(WuKongSounds.PERFECT_DODGE.get(), 0.5F, 0, 0);
                modifyStamina(event.getPlayerPatch().getOriginal(), 5.0F);
                event.setCanceled(true);
                event.setCanceled(true);
            }

        }));

       /* container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource && epicFightDamageSource.is(EpicFightDamageType.PARTIAL_DAMAGE))
                return;
            //閫€瀵告垚鍔熸櫘鏀讳粠绗?
            if (event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource) {
                epicFightDamageSource.setStunType(StunType.NONE);
            }
            if(event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(stepinch.get())){
                PacketRelay.sendToAll(PacketHandler.INSTANCE, new AddEntityAfterImageParticle(event.getPlayerPatch().getOriginal().getId()));
                container.getSkill().setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue() * 90); // 鑾峰緱澶ч噺妫嶅娍
                event.getPlayerPatch().playSound(WuKongSounds.PERFECT_DODGE.get(), 0.5F, 0, 0);//TODO 鏇挎崲
             //   BasicAttack.setComboCounterWithEvent(ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION, event.getPlayerPatch(), event.getPlayerPatch().getSkill(SkillSlots.BASIC_ATTACK), stepinch.get(), 4);
                modifyStamina(event.getPlayerPatch().getOriginal(), 5.0F);
                event.setCanceled(true);
                event.setCanceled(true);
            }
            DynamicAnimation current = event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation();
            if(current.equals(stepinch.get()) || current.equals(footage.get())){
                if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource){
                    epicFightDamageSource.setStunType(StunType.NONE);
                }
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS, event.getDamage() * 0.7F, attackerPatch);
                event.setResult(AttackResult.ResultType.BLOCKED);
                event.setCanceled(true);
            }
            event.getPlayerPatch().getOriginal().getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> {
                if (wkPlayer.getDamageReduce() > 0) {
                    LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                    this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS, event.getDamage() * (1 - wkPlayer.getDamageReduce()), attackerPatch);
                    event.setResult(AttackResult.ResultType.BLOCKED);
                    event.setCanceled(true);
                }
            });
        }));
*/


//

        super.onInitiate(container);
    }
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);

    }
    public void createRepelForAttackTarget(ServerPlayer player, Entity target, double knockbackStrength) {
        Vec3 playerPos = player.position();
        if (target instanceof LivingEntity) {
            Vec3 targetPos = target.position();
            double deltaX = targetPos.x - playerPos.x;
            double deltaZ = targetPos.z - playerPos.z;
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance > 0.1) {
                deltaX /= distance;
                deltaZ /= distance;
                target.push(deltaX * knockbackStrength, 0.0, deltaZ * knockbackStrength);
                target.setSecondsOnFire(10);
            }
        }
    }

    public void modifyStamina(LivingEntity livingentity, float staminaChange) {
        float currentStamina = livingentity.getEntityData().get(STAMINA);
        float newStamina = Math.max(0.0F, currentStamina + staminaChange);
        livingentity.getEntityData().set(STAMINA, newStamina);
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (container.getExecutor().isLogicalClient()) {
            boolean isKeyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            dataManager.setDataSync(WukongSkillDataKeys.Thrust_KEY_PRESSING.get(), isKeyDown, ((LocalPlayer) container.getExecutor().getOriginal()));


        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if(dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_STACK.get())){
                if(container.getStack() < 3){
                    this.setStackSynchronize(container, Math.min(container.getStack() + 2, 4));
                    serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                    dataManager.setData(WukongSkillDataKeys.Thrust_LAST_STACK.get(), container.getStack());
                }
                dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), 20, serverPlayer);
                dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_STACK.get(), false, serverPlayer);
            }

            dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_TIMER.get()) - 1, 0), serverPlayer);//娲剧敓閲嶅嚮
            if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get())!=0){
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get())-1, serverPlayer);//閫€瀵?
                }
            if (dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get())!=0){
                dataManager.setDataSync(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get(), dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get())-1, serverPlayer);
            }
            if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get())!=0){
                dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get())-1, serverPlayer);
            }

            if(container.getStack() > dataManager.getDataValue(WukongSkillDataKeys.Thrust_LAST_STACK.get())){
                serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_PLAY_SOUND.get(), false, serverPlayer);
                if (container.getStack()!=3 && dataManager.getDataValue(WukongSkillDataKeys.Thrust_KEY_PRESSING.get()) )
                    serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL_RISE03.get(), 2.0F, 2.0F);
            }

            dataManager.setData(WukongSkillDataKeys.Thrust_LAST_STACK.get(), container.getStack());
            dataManager.setDataSync(WukongSkillDataKeys.RED_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1, 0), serverPlayer);//浣跨敤鎶€鑳芥槦鏁版樉绀?
            if(dataManager.getDataValue(WukongSkillDataKeys.Thrust_IS_CHARGING.get()) ){
                if(!WukongWeaponCategories.isWeaponValid(serverPlayerPatch)){
                    dataManager.setDataSync(WukongSkillDataKeys.Thrust_IS_CHARGING.get(), false, serverPlayer);
                    this.setConsumptionSynchronize(container, 1);
                    this.setStackSynchronize(container, 0);
                    return;
                }
                if(container.getStack() < 3){
                    this.setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue());
                }
                if(!dataManager.getDataValue(WukongSkillDataKeys.Thrust_KEY_PRESSING.get())){
                    dataManager.setDataSync(WukongSkillDataKeys.Thrust_IS_CHARGING.get(), false, serverPlayer);
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
                    serverPlayerPatch.playAnimationSynchronized(animations[container.getStack()].get(), 0.0F);
                    dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack(), serverPlayer);
                    resetConsumption(container, serverPlayerPatch);
                }
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) > 0){
                if (dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())){
                    //WukongMoveset.LOGGER.info("鎼呮");
                    dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0, serverPlayer);
                    //寮€濮嬫悈
                    if (dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get()) > 0 && !dataManager.getDataValue(WukongSkillDataKeys.IS_REPEATING_DERIVE.get())) {
                        if (dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                            serverPlayerPatch.playAnimationSynchronized(juesick_start.get(), 0.15F);
                            dataManager.setDataSync(WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), true, serverPlayer);
                            dataManager.setDataSync(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get(), 0, serverPlayer);
                        }
                    }
                }else if (dataManager.getDataValue(WukongSkillDataKeys.THRUST_METERS_BACK.get())) {
                    //WukongMoveset.LOGGER.info("杩涘昂");
                    dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0, serverPlayer);
                    if (container.getStack() > 0 ){
                        serverPlayerPatch.playAnimationSynchronized(footage.get(), 0.0F);
                        this.setStackSynchronize(container, container.getStack() - 1);
                    }else{
                        serverPlayerPatch.playAnimationSynchronized(animations[0].get(), 0.0F);
                    }
                }

            }

            if (dataManager.getDataValue(WukongSkillDataKeys.IS_REPEATING_DERIVE.get())) {
                //鎵ｈ€愬姏
                if (!serverPlayer.isCreative()) {
                    //serverPlayerPatch.consumeStamina(Config.CHARGING_STAMINA_CONSUME.get().floatValue());
                    if (!serverPlayerPatch.hasStamina(0.1F)) {
                        serverPlayerPatch.playAnimationSynchronized(juesick_end.get(), 0.0F);
                        dataManager.setDataSync(WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), false, serverPlayer);
                    }
                }
                //閲嶇疆鍙€€瀵告椂闂?                //dataManager.setDataSync(ThrustHeavyAttack.CAN_FIRST_DERIVE, true, serverPlayerPatch.getOriginal());
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 30, serverPlayer);
                //鏉炬墜浜嗗垯鎾璭nd
                if (!dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                    serverPlayerPatch.playAnimationSynchronized(juesick_end.get(), 0.0F);
                    dataManager.setDataSync(WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), false, serverPlayer);
                }
            }




            if (container.getStack() < 1 && container.getResource() > container.getMaxResource() * 0.3) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 2 && container.getResource() > container.getMaxResource() * 0.5) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 3 && container.getResource() > container.getMaxResource() * 0.7) {
                breakProgress(serverPlayerPatch, container);
            }

            int current = dataManager.getDataValue(WukongSkillDataKeys.Thrust_CHARGED4_TIMER.get());
            if(current > 0){
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_CHARGED4_TIMER.get(), current - 1, serverPlayer);
            }
            float consumption = Config.CHARGING_SPEED.get().floatValue() / 5;
            if(current == 1 && container.isFull()){
                this.setStackSynchronize(container, 3);
                this.setConsumptionSynchronize(container, container.getMaxResource() - consumption);
            }
            if(current == 0 && container.getStack() >= 3 && container.getResource() > consumption + 0.1){
                this.setConsumptionSynchronize(container, container.getResource() - consumption);
            }




        }
    }
    public void breakProgress(ServerPlayerPatch serverPlayerPatch, SkillContainer container) {
        this.setConsumptionSynchronize(container, 0.1F);
        this.setStackSynchronize(container, container.getStack() + 1);
    }
    /**
     * copy from {@link yesman.epicfight.events.EntityEvents#attackEvent(LivingAttackEvent)}
     */
    public void processDamage(PlayerPatch<?> playerPatch, DamageSource damageSource, AttackResult.ResultType attackResult, float amount, @Nullable LivingEntityPatch<?> attackerPatch){
        AttackResult result = playerPatch != null ? AttackResult.of(attackResult, amount) : AttackResult.success(amount);
        if (attackerPatch != null) {
            attackerPatch.setLastAttackResult(result);
        }
        EpicFightDamageSource deflictedDamage = (damageSource instanceof EpicFightDamageSource epicFightDamageSource)? epicFightDamageSource : EpicFightDamageSources.fromVanillaDamageSource(damageSource);
        deflictedDamage.addRuntimeTag(EpicFightDamageType.PARTIAL_DAMAGE);
        if(playerPatch != null){
            playerPatch.getOriginal().hurt(deflictedDamage, result.damage);
        }
    }

    /**
     * 娓呯┖鑰愬姏骞舵挱绾㈠厜鍜岄煶鏁?     */
    private void resetConsumption(SkillContainer container, ServerPlayerPatch executer){
        if(container.getStack() > 0){
            int soundIndex = Math.min(container.getStack(), WuKongSounds.stackSounds.size()) - 1;
            executer.playSound(WuKongSounds.stackSounds.get(soundIndex).get(), 1, 1);
        } else {
            container.getDataManager().setDataSync(WukongSkillDataKeys.Thrust_PLAY_SOUND.get(), true, executer.getOriginal());
        }
        container.getDataManager().setDataSync(WukongSkillDataKeys.RED_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue(), executer.getOriginal());//閫氱煡瀹㈡埛绔浜孩鐏簡
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }
    /**
     * 鏍规嵁妫嶅紡鍜屾槦绾х敾鍥?     * 鍙栨秷鍘熸湰鐨勭粯鍒跺湪 {@link com.p1nero.wukong.mixin.BattleModeGuiMixin}
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        int stack = Mth.clamp(container.getStack(), 0, 4);
        int style = container.getExecutor().getHoldingItemCapability(InteractionHand.MAIN_HAND).getStyle(container.getExecutor()).universalOrdinal() - WukongStyles.SMASH.universalOrdinal();
        float cooldownRatio = !container.isFull() && !container.isActivated() ? container.getResource(1.0F) : 1.0F;
        int progress = Mth.clamp((int) Math.ceil(cooldownRatio * 40), 0, 40);
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation progressTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/progress/" + progress + ".png");
        ResourceLocation styleTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stance/" + style + "_0.png");
        ResourceLocation stackBgTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/ui" + stack + ".png");
        ResourceLocation stackTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/stack" + stack + ".png");
        ResourceLocation goldenLightTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/gold.png");
        ResourceLocation whiteLightTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/white.png");
        ResourceLocation redLightTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/red.png");
        guiGraphics.blit(progressTexture, pos.x - 12, pos.y - 12, 48, 48, 0.0F, 0.0F, 2, 2, 2, 2);
        drawTexture(guiGraphics,styleTexture, pos.x - 12, pos.y - 12);
        drawTexture(guiGraphics,stackBgTexture,pos.x - 12, pos.y - 12);
        Vec2i light1 = new Vec2i(pos.x - 14, pos.y + 3);
        Vec2i light2 = new Vec2i(pos.x - 5, pos.y + 1);
        Vec2i light3 = new Vec2i(pos.x + 4, pos.y - 5);
        List<Vec2i> lightList = List.of(light1, light2, light3);


        if (container.isFull()) {
            for (Vec2i lightPos : lightList) {
                drawTexture(guiGraphics,goldenLightTexture, lightPos.x, lightPos.y);
            }
        }
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.RED_TIMER.get()) > 0) {
            int star = Math.min(container.getDataManager().getDataValue(WukongSkillDataKeys.STARS_CONSUMED.get()), 3);
            if (star > 0) {
                for (int i = 0; i < star; i++) {
                    Vec2i lightPos = lightList.get(i);
                    drawTexture(guiGraphics,redLightTexture, lightPos.x, lightPos.y);
                }
            }
        }

        if (stack > 0) {
            for (int i = 0; i < Math.min(stack, 3); i++) {
                Vec2i lightPos = lightList.get(i);
                drawTexture(guiGraphics,whiteLightTexture, lightPos.x, lightPos.y);
            }
            drawTexture(guiGraphics,stackTexture, pos.x - 12, pos.y - 12);
        }


    }
    public void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 48, 48, 0.0F, 0.0F, 2, 2, 2, 2);
    }

    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }



    public static class Builder extends SkillBuilder<ThrustHeavyAttack> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider stepinch;
        protected StaticAnimationProvider footage;
        protected StaticAnimationProvider fengchuanhua;
        protected StaticAnimationProvider jumpAttackHeavy;
        protected StaticAnimationProvider juesick_start;
        protected StaticAnimationProvider juesick_loop;
        protected StaticAnimationProvider juesick_end;
        StaticAnimationProvider chargingAnimation;
        protected StaticAnimationProvider start;

        StaticAnimationProvider pre;

        public Builder() {
        }

        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public Builder setChargingAnimation(StaticAnimationProvider chargingAnimation) {
            this.chargingAnimation = chargingAnimation;
            return this;
        }

        public Builder setChargePreAnimation(StaticAnimationProvider pre) {
            this.pre = pre;
            return this;
        }
        /**
         * 濡傛灉鏄彲闀挎寜鐨勮鐢熷垯derive1灏辨槸pre鍔ㄧ敾锛屽叿浣撻€昏緫鍦ㄥ姩鐢婚偅閲屽垽鏂?         */
        public Builder setDeriveAnimations(StaticAnimationProvider stepinch, StaticAnimationProvider footage, StaticAnimationProvider fengchuanhua, StaticAnimationProvider juesick_start, StaticAnimationProvider juesick_loop, StaticAnimationProvider juesick_end) {
            this.stepinch = stepinch;
            this.fengchuanhua = fengchuanhua;
            this.footage = footage;
            this.juesick_start = juesick_start;
            this.juesick_loop = juesick_loop;
            this.juesick_end =juesick_end;
            return this;
        }
        /**
         * 0~4鏄熼噸鍑?         */
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        public Builder setStartAttacks(StaticAnimationProvider start) {
            this.start = start;
            return this;
        }
        public Builder setJumpAttackHeavy(StaticAnimationProvider jumpAttackHeavy){
            this.jumpAttackHeavy = jumpAttackHeavy;
            return this;
        }

    }

}
