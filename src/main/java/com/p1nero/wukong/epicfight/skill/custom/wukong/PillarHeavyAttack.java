package com.p1nero.wukong.epicfight.skill.custom.wukong;

import yesman.epicfight.skill.SkillBuilder;


import com.mojang.blaze3d.platform.Window;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.client.PillarFovPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.jetbrains.annotations.Nullable;

import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.api.client.input.InputManager;
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
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

/**
 *绔嬫閲嶅嚮
 */
public class PillarHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac114513");
     public static final int MAX_ANGLE_FOV = 74;
    public static final int MAX_FOVLJ = 0;
    protected final StaticAnimationProvider[] up;
     protected final StaticAnimationProvider[] start;//绔嬭捣鏉?     protected final StaticAnimationProvider[] up;//澧為珮锛?灏辨槸0to1锛?灏辨槸1to2
     protected final StaticAnimationProvider[] heavy;

    protected StaticAnimationProvider deriveAnimation1;
    protected StaticAnimationProvider deriveAnimation2;
    protected StaticAnimationProvider hotwheel;
    protected StaticAnimationProvider deriveEnd;

    @Override
    public  List<StaticAnimationProvider> getHeavyAttacks(){
        List<StaticAnimationProvider> staticAnimations =new java.util.ArrayList<>(List.of(heavy));
        staticAnimations.add(deriveAnimation2);
        return staticAnimations;
    }

    public static Builder createChargedAttack(){
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);

    }

    public PillarHeavyAttack(Builder builder) {
        super(builder);
        this.start = builder.start;
        this.up = builder.up;
        this.heavy = builder.heavy;

        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
        hotwheel = builder.hotwheel;
        deriveEnd= builder.deriveEnd;

    }


    /**
     * 鍦ㄨ鏃跺懆鏈熷唴浣跨敤鎶€鑳芥墠绠椾娇鐢ㄨ鐢燂紝鍚﹀垯瑙嗕负閲嶅嚮
     * 闀挎寜寰幆绗竴娈佃鐢熺殑鍒ゆ柇鍦▄@link PillarHeavyAttack#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack());//0鏄熶篃鏄槦锛?
        boolean stackConsumed = container.getStack() > 0;
        if ( container.getStack()==4){
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
            if (container.getStack()>0){
                sendFovAnimation(player, 7F, 31, container.getStack());
            }
            executer.playAnimationSynchronized(start[container.getStack()].get(), 0F);
        }else if(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get()) > 0 ) {
            executer.playAnimationSynchronized(start[container.getStack()].get(), 0F);
            dataManager.setData(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(), 0);
            this.setStackSynchronize(container, container.getStack() - 1);
        }else if(dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0 && stackConsumed ){
            dataManager.setData(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get(), true);
            executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);
            dataManager.setData(WukongSkillDataKeys.DERIVE_TIMER.get(), 0);
            this.setStackSynchronize(container, container.getStack() - 1);
        }else if(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get()) > 0  && stackConsumed ){
            dataManager.setDataSync(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get(), 0);
            executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
            this.setStackSynchronize(container, container.getStack() - 1);
            executer.playAnimationSynchronized(deriveAnimation2.get(), 0F);
        }else {
            //閲嶅嚮寮€濮嬭搫鍔?
            if(!dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get()) && checkSpace(player, container.getStack() * 2)){
                dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                if (container.getStack()>0){
                    sendFovAnimation(player, 7F, 31, container.getStack());
                }
                executer.playAnimationSynchronized(start[container.getStack()].get(), 0F);
            }
        }
        super.executeOnServer(container, args);

    }
    private static void sendFovAnimation(ServerPlayer player, float increaseAmount, int durationTicks, int repeatTimes) {
        PacketRelay.sendToPlayer(
                PacketHandler.INSTANCE,
                new PillarFovPacket(increaseAmount, durationTicks, Math.max(1, repeatTimes)),
                player
        );
    }


    private void resetConsumption(SkillContainer container, ServerPlayerPatch executer){
        if(container.getStack() > 0){
            int cnt = container.getStack();

        } else {
            //  container.getDataManager().setDataSync(MoreBattlesSkillDataKeys.PLAY_SOUND.get(), true, executer.getOriginal());
        }
        //  container.getDataManager().setDataSync(MoreBattlesSkillDataKeys.RED_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue(), executer.getOriginal());//閫氱煡瀹㈡埛绔浜孩鐏簡
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }
    @Override
    public void onInitiate(SkillContainer container) {
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            if(event.getPlayerPatch().getAnimator().getPlayerFor(null).getAnimation().equals(WukongAnimations.PILLAR_HEAVY_FENGYUNZHUAN.get())){
                if (container.getStack()<4){
                    container.getSkill().setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue());
                }
            }
        }));

        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID, (event -> {
            //闀挎寜鏈熼棿绂佹璺宠穬
            if (event.getPlayerPatch().isEpicFightMode() && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                // 弃用API迁移: getMovementInput()改为getInputState(), 通过InputManager.setInputState应用回原版输入(已核实与直接改Input字段等价)
                InputManager.setInputState(event.getInputState().withJumping(false));
            }
            //钃勫姏鏈熼棿绂佺敤绉诲姩
            if (event.getPlayerPatch().isEpicFightMode() && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                // 弃用API迁移: 同上, 并以KeyMapping.setDown替代已弃用的ControlEngine.setKeyBind(其内部就是该调用)
                InputManager.setInputState(event.getInputState()
                        .withForwardImpulse(0.0F)
                        .withLeftImpulse(0.0F)
                        .withDown(false)
                        .withUp(false)
                        .withLeft(false)
                        .withRight(false)
                        .withJumping(false)
                        .withSneaking(false));
                LocalPlayer clientPlayer = event.getPlayerPatch().getOriginal();
                clientPlayer.setSprinting(false);
                clientPlayer.sprintTriggerTime = -1;
                Minecraft mc = Minecraft.getInstance();
                mc.options.keySprint.setDown(false);
            }
        }));
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            if (event.getDamageSource().is(DamageTypes.FALL) && container.getDataManager().getDataValue(WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                event.setCanceled(true);
                event.setCanceled(true);
                event.setResult(AttackResult.ResultType.MISSED);
                event.getPlayerPatch().getOriginal().resetFallDistance();
                container.getDataManager().setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), false);
            }
            //闇镐綋鍑忎激
            if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource && epicFightDamageSource.is(EpicFightDamageType.PARTIAL_DAMAGE))
                return;
            float damageReduce = container.getDataManager().getDataValue(WukongSkillDataKeys.DAMAGE_REDUCE.get());
            if(damageReduce > 0){
                if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource){
                    epicFightDamageSource.setStunType(StunType.NONE);
                }
                
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS,(1 - damageReduce) * event.getDamage(), attackerPatch);
                event.setResult(AttackResult.ResultType.MISSED);
                event.setCanceled(true);
            }
            //闃叉鍧犳満

            if (event.getDamageSource().is(DamageTypes.FALL) && container.getDataManager().getDataValue(WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS, event.getDamage() * 0.4F, attackerPatch);
                event.setResult(AttackResult.ResultType.BLOCKED);
                event.setCanceled(true);
                container.getDataManager().setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), false);
            }
            event.getPlayerPatch().getOriginal().getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> {
                if (wkPlayer.getDamageReduce() > 0) {
                    if (event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource) {
                        epicFightDamageSource.setStunType(StunType.NONE);
                    }
                    LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                    this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS, event.getDamage() * (1 - wkPlayer.getDamageReduce()), attackerPatch);
                    event.setResult(AttackResult.ResultType.BLOCKED);
                    event.setCanceled(true);
                }
            });
        }));
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID, (event -> {
                    ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
                    ServerPlayer player = serverPlayerPatch.getOriginal();
                    CapabilityItem capabilityItem = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
                    if(!WukongWeaponCategories.isWeaponValid(event.getPlayerPatch())){
                        return;
                    }
                    //鏅敾鍚庣珛鍗冲彸閿彲浠ヨ鐢?                    var autoAnimations = capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                    var autoAnimations = capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                    for(int i = 0; i < autoAnimations.size(); i++){
                        if(autoAnimations.get(i).equals(event.getAnimation()) && i < 4){
                            container.getDataManager().setDataSync(WukongSkillDataKeys.DERIVE_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue());
                            return;
                        }
                    }
                }));

        super.onInitiate(container);
    }
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);

    }
    /**
     * 鍒ゆ柇绌洪棿鏄惁瓒冲绔?     * @param height 浠ョ帺瀹惰剼搴曞紑濮嬪線涓婇渶瑕佸嚑鏍?     */
    public static boolean checkSpace(ServerPlayer serverPlayer, int height) {
        // 鑾峰彇鐜╁鎵€鍦ㄧ殑鏈嶅姟鍣ㄤ笘鐣?
        ServerLevel serverLevel = serverPlayer.serverLevel();
        // 寰幆妫€鏌ョ帺瀹跺ご椤?`height` 楂樺害鍐呯殑姣忎釜浣嶇疆
        for (int i = 1; i <= height; i++) {
            // 妫€鏌ョ帺瀹跺綋鍓嶆墍鍦ㄤ綅缃笂鏂?`i` 涓崟浣嶇殑鏂瑰潡鐘舵€?
            if (!serverLevel.getBlockState(serverPlayer.getOnPos().above(i)).is(Blocks.AIR)) {
                // 濡傛灉涓嶆槸绌烘皵锛岃繑鍥?false
                return false;
            }
        }

        // 濡傛灉妫€鏌ュ畬鎵€鏈夐珮搴﹀悗閮戒负绌烘皵锛屽垯杩斿洖 true
        return true;
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
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if(container.getExecutor().isLogicalClient()){
            //KEY_PRESSING鐢ㄤ簬鏈嶅姟绔垽鏂槸鍚︾户缁挱鍔ㄧ敾
            boolean isKeyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            dataManager.setDataSync(WukongSkillDataKeys.KEY_PRESSING.get(), isKeyDown);
        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            //灞傛暟鍙樺寲妫€娴嬩互鎾煶鏁?
            if(container.getStack() > dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())){
                serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                dataManager.setDataSync(WukongSkillDataKeys.PLAY_SOUND.get(), false);
                if(dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())){
                    if (!dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get())&& dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())<3){
                        serverPlayerPatch.playAnimationSynchronized(up[dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())].get(), 0.1F);
                    }
                }
            }
            dataManager.setData(WukongSkillDataKeys.LAST_STACK.get(), container.getStack());
            //鏇存柊璁℃椂鍣?
            dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get()) - 1, 0));//娲剧敓閲嶅嚮
            dataManager.setDataSync(WukongSkillDataKeys.RED_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1, 0));//浣跨敤鎶€鑳芥槦鏁版樉绀?
            if(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_STACK.get()) ){
                if(container.getStack() < 3){
                    this.setStackSynchronize(container, Math.min(container.getStack() + 2, 4));
                    serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                    dataManager.setData(WukongSkillDataKeys.LAST_STACK.get(), container.getStack());
                }
                dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(), 18);
                dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_STACK.get(), false);
            }

            if(dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0){
                dataManager.setDataSync(WukongSkillDataKeys.DERIVE_TIMER.get(), dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) - 1);//鍒囨墜鎶€鏈夋晥鏃堕棿璁＄畻
            }
            if(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get()) > 0){
                dataManager.setDataSync(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get(), dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get()) - 1);//鍒囨墜鎶€鏈夋晥鏃堕棿璁＄畻
            }

            if(dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())){
                //闃叉鍒囩墿鍝佷骇鐢熺殑bug
                if(!WukongWeaponCategories.isWeaponValid(serverPlayerPatch)){
                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    this.setConsumptionSynchronize(container, 1);
                    this.setStackSynchronize(container, 0);
                    return;
                }
                //钃勫姏鐨勫姞鏉?
                if(container.getStack() < 3&&dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())){
                    this.setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue());
                }
                if(!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())){

                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);//MAN
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);

                    serverPlayerPatch.playAnimationSynchronized(heavy[container.getStack()].get(), 0.0F);//鏈夊嚑鏄熷氨鍑犳槦閲嶅嚮
                    dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack());//璁剧疆娑堣€楁槦鏁帮紝鏂逛究瀹㈡埛绔粯鍒?
                    resetConsumption(container, serverPlayerPatch);
                }

            }

            // WukongMoveset.LOGGER.info("绔嬫PILLAR_JIANGHAIFAN_TIMER: {}",dataManager.getDataValue(WukongSkillDataKeys.JIANGHAIFAN_TIMER.get())) ;

            if(dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get())){
                if(!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())){
                    dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get(), false);
                    serverPlayerPatch.playAnimationSynchronized(deriveEnd.get(), 0.0F);
                }
            }

            //鐮存潯鍒欏姞stack娓呯┖钃勫姏鏉?
            if (container.getStack() < 1 && container.getResource() > container.getMaxResource() * 0.3) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 2 && container.getResource() > container.getMaxResource() * 0.5) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 3 && container.getResource() > container.getMaxResource() * 0.7) {
                breakProgress(serverPlayerPatch, container);
            }
            //鍥涜搫鐨勬帀妫嶅娍鏃堕棿鍒ゆ柇
            int current = dataManager.getDataValue(WukongSkillDataKeys.CHARGED4_TIMER.get());
            if(current > 0){
                dataManager.setDataSync(WukongSkillDataKeys.CHARGED4_TIMER.get(), current - 1);
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
    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    /**
     * 根据棍式和星级画图
     * 本方法完全重写 Epic Fight 默认的技能图标绘制, 战斗模式 HUD 仅显示此自定义画面
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
        ResourceLocation progressTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/progress/" + progress + ".png");
        ResourceLocation styleTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stance/" + style + "_0.png");
        ResourceLocation stackBgTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/ui" + stack + ".png");
        ResourceLocation stackTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/stack" + stack + ".png");
        ResourceLocation goldenLightTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/gold.png");
        ResourceLocation whiteLightTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/white.png");
        ResourceLocation redLightTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/red.png");
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

    @Override
    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }

    public static class Builder extends SkillBuilder<PillarHeavyAttack> {
        protected StaticAnimationProvider[] start;
        protected StaticAnimationProvider[] up;
        protected StaticAnimationProvider[] heavy;
        protected StaticAnimationProvider derive1;
        protected StaticAnimationProvider derive2;
        protected StaticAnimationProvider deriveLoop;
        protected StaticAnimationProvider deriveEnd;
        protected StaticAnimationProvider hotwheel;

        StaticAnimationProvider pre;

        public Builder() {
        }

        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder setActivateType(ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public Builder setChargePreAnimation(StaticAnimationProvider pre) {
            this.pre = pre;
            return this;
        }



        /**
         * 绔嬫钃勫姏鍓嶆憞
         */
        public Builder setStartAnimations(StaticAnimationProvider... animationProviders) {
            this.start = animationProviders;
            return this;
        }
        /**
         * 绔嬫钃勫姏0鍒?璞嗚宫鑵?         */
        public Builder setUpAnimations(StaticAnimationProvider... animationProviders) {
            this.up = animationProviders;
            return this;
        }
        /**
         * 0~4鏄熼噸鍑?         */
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.heavy = animationProviders;
            return this;
        }
        public Builder setDeriveAnimations(StaticAnimationProvider derivePre, StaticAnimationProvider deriveLoop, StaticAnimationProvider deriveEnd, StaticAnimationProvider derive2) {
            this.derive1 = derivePre;
            this.deriveLoop = deriveLoop;
            this.deriveEnd = deriveEnd;
            this.derive2 = derive2;
            return this;
        }


    }

}
