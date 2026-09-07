package com.p1nero.wukong.epicfight.skill.custom.wukong;

import yesman.epicfight.skill.SkillBuilder;

import com.mojang.blaze3d.platform.Window;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.custom.WukongDodgeAnimation;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.item.WukongItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.ValueModifier;
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
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

/**
 * 鍔堟閲嶅嚮THRUSTHeavyAttack
 * ThrustHeavyAttack
 */
public class SmashHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac114512");
    @NotNull
    protected final StaticAnimationProvider[] animations;//0~4鍏辨湁浜旂閲嶅嚮
    public static final int MAX_CHARGED4_TICKS = 300;//15s
    protected StaticAnimationProvider deriveAnimation1;
    protected StaticAnimationProvider deriveAnimation2;
    protected StaticAnimationProvider deriveAnimation3;
    @NotNull
    protected StaticAnimationProvider jumpAttackHeavy;
    @NotNull
    protected StaticAnimationProvider chargePre;

    @Override
    public List<StaticAnimationProvider> getHeavyAttacks(){
        List<StaticAnimationProvider> staticAnimations =new java.util.ArrayList<>(List.of(animations));
        staticAnimations.add(deriveAnimation2);
        return staticAnimations;
    }


    public static Builder createChargedAttack(){
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    public SmashHeavyAttack(Builder builder) {
        super(builder);
        chargePre = builder.pre;

        this.animations = builder.animationProviders;
        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
        deriveAnimation3 = builder.derive3;
        jumpAttackHeavy = builder.jumpAttackHeavy;
    }


    /**
     * 鍦ㄨ鏃跺懆鏈熷唴浣跨敤鎶€鑳芥墠绠椾娇鐢ㄨ鐢燂紝鍚﹀垯瑙嗕负閲嶅嚮
     * 闀挎寜寰幆绗竴娈佃鐢熺殑鍒ゆ柇鍦▄@link SmashHeavyAttack#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack());//0鏄熶篃鏄槦锛?
        if(dataManager.getDataValue(WukongSkillDataKeys.CAN_JUMP_HEAVY.get()) && !player.onGround()){
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);//鏀鹃噷闈紝闃叉鐬庢寜鎶€鑳介敭灏遍槻鍧犳満鐨刡ug
            //璺宠穬鏀诲嚮锛屼篃娑堣€楁墍鏈夋鍔?
            dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), false);
            if(container.getStack() > 0){//0鏄熸槸null浼氫腑鏂?                executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
            }
            executer.playAnimationSynchronized(jumpAttackHeavy.get(), 0.15F);
            resetConsumption(container, executer, false);
        } else if(player.onGround()){
            //濡傛灉鐢ㄤ簡鏄熷垯瑕佸己鍖栬鐢?
            boolean stackConsumed = container.getStack() > 0;
            if(dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0  && !container.isFull()){//鏈夋槦鎵嶈兘鐢ㄧ牬妫嶅紡锛屼笖婊℃槦鐩存帴鏀惧ぇ锛堜篃闃瞓ug锛?
            if(dataManager.getDataValue(WukongSkillDataKeys.CAN_FIRST_DERIVE.get())){
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                    executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
                    this.setStackSynchronize(container, container.getStack() - 1);
                    executer.playAnimationSynchronized(deriveAnimation1.get(), 0.2F);
                }else if(dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_DERIVE.get())){
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                    executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
                    this.setStackSynchronize(container, container.getStack() - 1);
                    executer.playAnimationSynchronized(deriveAnimation2.get(), 0.2F);
                }
            } else if (container.isFull() && deriveAnimation3 != null && isWearingGreatSageSet(player)) {
                executer.playAnimationSynchronized(deriveAnimation3.get(), 0.2F);
            } else {
                //閲嶅嚮锛屾秷鑰楁墍鏈夋槦锛屽紑濮嬭搫鍔涳紝鏉炬墜鍦ㄥ鎴风鍒ゆ柇
                if(!dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())){
                    executer.playAnimationSynchronized(chargePre.get(), 0.2F);

                }
            }

        }

        super.executeOnServer(container, args);
    }

    private static boolean isWearingGreatSageSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(WukongItems.DASHENG_H.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(WukongItems.DASHENG_C.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(WukongItems.DASHENG_L.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(WukongItems.DASHENG_F.get());
    }

    /**
     * 娓呯┖鑰愬姏骞舵挱绾㈠厜鍜岄煶鏁?     * @param playSound 濡傛灉鏄€氳繃钃勫姏鑰岄噴鏀剧殑灏变笉鎾煶鏁?     */
    private void resetConsumption(SkillContainer container, ServerPlayerPatch executer, boolean playSound){
        if(playSound && container.getStack() > 0){
            int soundIndex = Math.min(container.getStack(), WuKongSounds.stackSounds.size()) - 1;
            executer.playSound(WuKongSounds.stackSounds.get(soundIndex).get(), 1, 1);
        } else {
            container.getDataManager().setDataSync(WukongSkillDataKeys.PLAY_SOUND.get(), true);
        }
        container.getDataManager().setDataSync(WukongSkillDataKeys.RED_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue());//閫氱煡瀹㈡埛绔浜孩鐏簡
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }

    @Override
    public void onInitiate(SkillContainer container) {

        //闀挎寜鏈熼棿绂佹璺宠穬
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID, (event -> {
            if (event.getPlayerPatch().isEpicFightMode() && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                // 弃用API迁移: getMovementInput()改为getInputState(), 通过InputManager.setInputState应用回原版输入(已核实与直接改Input字段等价)
                InputManager.setInputState(event.getInputState().withJumping(false));
            }
        }));

        //鎴愬姛璇嗙牬鍔犳鍔匡紝骞堕噸缃櫘鏀昏鏁板櫒锛屼笅娆′粠涓夋鏅敾寮€濮?
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
            if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource && epicFightDamageSource.is(EpicFightDamageType.PARTIAL_DAMAGE))
                return;
            if(container.getDataManager().getDataValue(WukongSkillDataKeys.IS_IN_SPECIAL_ATTACK.get())){
                if(!container.getDataManager().getDataValue(WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get())){
                    container.getSkill().setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue() * 30);//鑾峰緱澶ч噺妫嶅娍
                    container.getDataManager().setDataSync(WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get(), true);
                }
                BasicAttack.setComboCounterWithEvent(ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION, event.getPlayerPatch(), event.getPlayerPatch().getSkill(SkillSlots.BASIC_ATTACK), deriveAnimation1.get(), 2);
                event.setCanceled(true);
                event.setCanceled(true);
            }

            float damageReduce = container.getDataManager().getDataValue(WukongSkillDataKeys.DAMAGE_REDUCE.get());
            //闇镐綋
            if(damageReduce > 0){
                if(event.getDamageSource() instanceof EpicFightDamageSource epicFightDamageSource){
                    epicFightDamageSource.setStunType(StunType.NONE);
                }
                
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getEntity(), LivingEntityPatch.class);
                this.processDamage(event.getPlayerPatch(), event.getDamageSource(), AttackResult.ResultType.SUCCESS,(1 - damageReduce) * event.getDamage(), attackerPatch);
                event.setResult(AttackResult.ResultType.MISSED);
                event.setCanceled(true);
            }

            //闃叉鍧犳満 FIXME
            if (event.getDamageSource().is(DamageTypes.FALL) && container.getDataManager().getDataValue(WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                System.out.println("man!");
                event.setCanceled(true);
                event.setCanceled(true);
                event.setResult(AttackResult.ResultType.MISSED);
                event.getPlayerPatch().getOriginal().resetFallDistance();
                container.getDataManager().setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), false);
            }
        }));

        container.getExecutor().getEventListener().addEventListener(
                PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID, (event -> {
                    ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
                    ServerPlayer player = serverPlayerPatch.getOriginal();
                    CapabilityItem capabilityItem = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
                    if(!WukongWeaponCategories.isWeaponValid(event.getPlayerPatch())){
                        return;
                    }

                    //钃勫姏鐨勬椂鍊欏仛鍔ㄤ綔鏄潪娉曠殑锛屽簲璇ユ竻绌烘鍔?
                    if(container.getDataManager().getDataValue(WukongSkillDataKeys.IS_CHARGING.get()) && !event.getAnimation().equals(chargePre.get()) && !(event.getAnimation().get() instanceof WukongDodgeAnimation)){
                        this.setConsumptionSynchronize(container, 1);
                        this.setStackSynchronize(container, 0);
                        container.getDataManager().setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    }

                    //鏅敾鍚庣珛鍗冲彸閿彲浠ヨ鐢?                    var autoAnimations = capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                    var autoAnimations = capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                    for(int i = 0; i < autoAnimations.size(); i++){
                        if(autoAnimations.get(i).equals(event.getAnimation()) && i < 4){
                            container.getDataManager().setDataSync(WukongSkillDataKeys.CAN_FIRST_DERIVE.get(), true);
                            container.getDataManager().setDataSync(WukongSkillDataKeys.DERIVE_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue());
                            return;
                        }
                    }
                }));

        //鍒锋柊鍥涜搫璁℃椂鍣紝璇嗙牬鎵撲腑鍒欏彲鎺ヤ簩娈?
        container.getExecutor().getEventListener().addEventListener(
                PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID, (event -> {
                    ServerPlayer player = event.getPlayerPatch().getOriginal();
                    if(container.isFull()){
                        container.getDataManager().setDataSync(WukongSkillDataKeys.CHARGED4_TIMER.get(), MAX_CHARGED4_TICKS);
                    }
                    if(event.getDamageSource().getAnimation().equals(deriveAnimation1.get())){
                        container.getDataManager().setDataSync(WukongSkillDataKeys.CAN_SECOND_DERIVE.get(), true);
                        container.getDataManager().setDataSync(WukongSkillDataKeys.DERIVE_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue());
                    }
                }));

        //鏍规嵁鏄熸暟鏀硅烦璺冮噸鍑诲拰鐮淬€佹柀妫嶅紡浼ゅ
        container.getExecutor().getEventListener().addEventListener(
                PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID, (event -> {
                    int starCnt = container.getDataManager().getDataValue(WukongSkillDataKeys.STARS_CONSUMED.get());
                    if(event.getDamageSource().getAnimation().equals(jumpAttackHeavy.get())){
                        float mul = switch (starCnt) {
                            case 1 -> 3;
                            case 2 -> 4.5F;
                            case 3 -> 6.2F;
                            case 4 -> 8.75F;
                            default -> 1.45F;
                        };
                        event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(mul));
                    } else if(event.getDamageSource().getAnimation().equals(deriveAnimation1.get())){
                        float mul = starCnt == 0 ? 1.0F : 1.96F;
                        event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(mul));
                    } else if(event.getDamageSource().getAnimation().equals(deriveAnimation2.get())){
                        float mul = switch (starCnt) {
                            case 1 -> 4.7F;
                            case 2 -> 4.9F;
                            case 3, 4 -> 5.1F;
                            default -> 4.48F;
                        };
                        event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(mul));
                    }
                    //瀵瑰€掑湴鐨勬晫浜轰笉鏂藉姞纭洿
                    event.getTarget().getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(entityPatch -> {
                        if(entityPatch instanceof LivingEntityPatch<?> livingEntityPatch){
                            if(livingEntityPatch.getEntityState().knockDown()){
                                event.getDamageSource().setStunType(StunType.NONE);
                            }
                        }
                    });
                }));

        super.onInitiate(container);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.FALL_EVENT, EVENT_UUID);
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
            }
            dataManager.setData(WukongSkillDataKeys.LAST_STACK.get(), container.getStack());

            //璺抽噸鍑荤殑鍒ゆ柇
            if(!serverPlayer.onGround()){
                dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), true);
            } else if(dataManager.getDataValue(WukongSkillDataKeys.CAN_JUMP_HEAVY.get())){
                dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), false);
            }

            //鏇存柊璁℃椂鍣?
            dataManager.setDataSync(WukongSkillDataKeys.RED_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1, 0));//浣跨敤鎶€鑳芥槦鏁版樉绀?
            dataManager.setDataSync(WukongSkillDataKeys.DERIVE_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) - 1, 0));//鍒囨墜鎶€鏈夋晥鏃堕棿璁＄畻
            if(dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) <= 0){
                dataManager.setDataSync(WukongSkillDataKeys.CAN_FIRST_DERIVE.get(), false);
                dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_DERIVE.get(), false);
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
                if(container.getStack() < 3){
                    this.setConsumptionSynchronize(container, container.getResource() + Config.CHARGING_SPEED.get().floatValue());
                }
                //鏉炬墜鍒欐竻绌烘鍔挎墦閲嶅嚮
                if(!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())){
                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);//MAN
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
                    serverPlayerPatch.playAnimationSynchronized(animations[container.getStack()].get(), 0.0F);//鏈夊嚑鏄熷氨鍑犳槦閲嶅嚮
                    dataManager.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack());//璁剧疆娑堣€楁槦鏁帮紝鏂逛究瀹㈡埛绔粯鍒?
                    resetConsumption(container, serverPlayerPatch, true);
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

    public static class Builder extends SkillBuilder<SmashHeavyAttack> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider derive1;
        protected StaticAnimationProvider derive2;
        protected StaticAnimationProvider derive3;
        protected StaticAnimationProvider jumpAttackHeavy;
        StaticAnimationProvider chargingAnimation;
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
         * 0~4鏄熼噸鍑?         */
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        /**
         * 濡傛灉鏄彲闀挎寜鐨勮鐢熷垯derive1灏辨槸pre鍔ㄧ敾锛屽叿浣撻€昏緫鍦ㄥ姩鐢婚偅閲屽垽鏂?         */
        public Builder setDeriveAnimations(StaticAnimationProvider derive1, StaticAnimationProvider derive2, StaticAnimationProvider derive3) {
            this.derive1 = derive1;
            this.derive2 = derive2;
            this.derive3 = derive3;
            return this;
        }

        public Builder setJumpAttackHeavy(StaticAnimationProvider jumpAttackHeavy){
            this.jumpAttackHeavy = jumpAttackHeavy;
            return this;
        }
    }

}
