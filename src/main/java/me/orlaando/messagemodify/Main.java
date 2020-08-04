package me.orlaando.messagemodify;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
    private interface Processor {
        public Object process(Object value, Object parent);
    }

    @Override public void onEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(this, PacketType.Play.Server.CHAT) {
                    private JSONParser parser = new JSONParser();

                    @Override public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();
                        StructureModifier<WrappedChatComponent> components = packet.getChatComponents();

                        try {

                            if (components == null || components.size() < 1) return;

                            Object data = parser.parse(components.read(0).getJson());
                            final boolean[] result = new boolean[1];

                            transformPrimitives(data, null, new Processor() {

                                @SuppressWarnings("unchecked")
                                @Override public Object process(Object value, Object parent) {
                                    if (value instanceof String) {
                                        String stripped = ChatColor.stripColor((String) value);

                                        if (stripped.contains("There are")) {
                                            result[0] = true;

                                            // Add color to the parent object
                                            if (parent instanceof JSONObject) {
                                                ((JSONObject) parent).put("color", "red");
                                            }
                                            return "lol loser";
                                        }
                                    }
                                    return value;
                                }
                            });

                            // Write back the changed string
                            if (result[0]) {
                                components.write(0, WrappedChatComponent.fromJson(JSONValue.toJSONString(data)));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private Object transformPrimitives(Object value, Object parent, Processor processor) {
        // Check its type
        if (value instanceof JSONObject) {
            return transformPrimitives((JSONObject) value, processor);
        } else if (value instanceof JSONArray) {
            return transformPrimitives((JSONArray) value, processor);
        } else {
            return processor.process(value, parent);
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject transformPrimitives(JSONObject source, Processor processor) {
        for (Object key : source.keySet().toArray()) {
            Object value = source.get(key);
            source.put(key, transformPrimitives(value, source, processor));
        }
        return source;
    }

    @SuppressWarnings("unchecked")
    private JSONArray transformPrimitives(JSONArray source, Processor processor) {
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            source.set(i, transformPrimitives(value, source, processor));
        }
        return source;
    }

    @Override public void onDisable() {}

}
