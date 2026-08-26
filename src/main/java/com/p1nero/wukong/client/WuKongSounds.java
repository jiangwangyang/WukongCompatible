package com.p1nero.wukong.client;

import com.p1nero.wukong.WukongMoveset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class WuKongSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WukongMoveset.MOD_ID);

    public static RegistryObject<SoundEvent> HAOMAO_SWSF = registerSoundEvent("haomao_swsf");
    public static RegistryObject<SoundEvent> HAOMAO_SWSF_END = registerSoundEvent("haomao_swsf_end");
    public static RegistryObject<SoundEvent> SHENFA_JXSQ_CAST = registerSoundEvent("shenfa_jxsq_cast");
    public static RegistryObject<SoundEvent> SHENFA_JXSQ_END = registerSoundEvent("shenfa_jxsq_end");
    public static RegistryObject<SoundEvent> SHENFA_TTTB = registerSoundEvent("shenfa_tttb");
    public static RegistryObject<SoundEvent> FASHU_DSS = registerSoundEvent("fashu_dss");
    public static RegistryObject<SoundEvent> FASHU_ASS = registerSoundEvent("fashu_ass");

    public static RegistryObject<SoundEvent> XULI_ATTACK_4 = registerSoundEvent("xuli_attack_4");

    public static RegistryObject<SoundEvent> XULI_DING_SOU = registerSoundEvent("xuli_ding_sou");
    public static RegistryObject<SoundEvent> XULI_LEVEL_RISE03 = registerSoundEvent("xuli_level_rise03");
    public static RegistryObject<SoundEvent> XULI_LEVEL_DONE01 = registerSoundEvent("xuli_level_done01");
    public static RegistryObject<SoundEvent> XULI_LEVEL_DONE02 = registerSoundEvent("xuli_level_done02");
    public static RegistryObject<SoundEvent> XULI_LEVEL_DONE03 = registerSoundEvent("xuli_level_done03");
    public static RegistryObject<SoundEvent> XULI_LEVEL_DONE04 = registerSoundEvent("xuli_level_done04");
    public static List<RegistryObject<SoundEvent>> XULI_LEVEL = new ArrayList<>();
    static {
        XULI_LEVEL.add(XULI_LEVEL_DONE01);
        XULI_LEVEL.add(XULI_LEVEL_DONE02);
        XULI_LEVEL.add(XULI_LEVEL_DONE03);
        XULI_LEVEL.add(XULI_LEVEL_DONE04);
    }



    public static RegistryObject<SoundEvent> PERFECT_FYZ = registerSoundEvent("lgfyz");
    public static RegistryObject<SoundEvent> PERFECT_DODGE = registerSoundEvent("perfect_dodge");
    public static RegistryObject<SoundEvent> PERFECT_FENGCHUANHUA = registerSoundEvent("fengchuanhua");
    public static RegistryObject<SoundEvent> TTTB = registerSoundEvent("tttbyx");
    public static RegistryObject<SoundEvent> FS_ASF = registerSoundEvent("fashu_anshenfa");

    public static RegistryObject<SoundEvent> FASHU_DING1 = registerSoundEvent("fashu_ding1");
    public static RegistryObject<SoundEvent> FASHU_DING2 = registerSoundEvent("fashu_ding2");
    public static List<RegistryObject<SoundEvent>> DINGSOUNDS = new ArrayList<>();
    static {
        DINGSOUNDS.add(FASHU_DING1);
        DINGSOUNDS.add(FASHU_DING2);
    }


    public static RegistryObject<SoundEvent> STAFF1 = registerSoundEvent("staff_auto1");
    public static RegistryObject<SoundEvent> STAFF2 = registerSoundEvent("staff_auto2");
    public static RegistryObject<SoundEvent> STAFF4 = registerSoundEvent("staff_auto4");
    public static RegistryObject<SoundEvent> STAFF5 = registerSoundEvent("staff_auto5");
    public static RegistryObject<SoundEvent> wave5 = registerSoundEvent("wave5");


    public static RegistryObject<SoundEvent> STACK1 = registerSoundEvent("stack1");
    public static RegistryObject<SoundEvent> STACK2 = registerSoundEvent("stack2");
    public static RegistryObject<SoundEvent> STACK3 = registerSoundEvent("stack3");
    public static RegistryObject<SoundEvent> STACK4 = registerSoundEvent("stack4");
    public static RegistryObject<SoundEvent> HIT_GROUND = registerSoundEvent("hit_ground");
    public static RegistryObject<SoundEvent> START_CHARGE = registerSoundEvent("start_charge");
    public static List<RegistryObject<SoundEvent>> stackSounds = new ArrayList<>();
    static {
        stackSounds.add(STACK1);
        stackSounds.add(STACK2);
        stackSounds.add(STACK3);
        stackSounds.add(STACK4);
    }
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(WukongMoveset.MOD_ID, name)));
    }

}