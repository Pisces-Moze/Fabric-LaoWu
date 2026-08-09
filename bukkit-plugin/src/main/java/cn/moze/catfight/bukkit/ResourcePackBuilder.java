package cn.moze.catfight.bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ResourcePackBuilder {
    private static final String[] VARIANTS = {
        "tabby", "black", "red", "siamese", "british_shorthair", "calico",
        "persian", "ragdoll", "white", "jellie", "all_black"
    };
    private static final int[] PIECES = {
        ModelIds.HEAD, ModelIds.JAW, ModelIds.BODY_1, ModelIds.BODY_2,
        ModelIds.BODY_3, ModelIds.BODY_4, ModelIds.TAIL_1, ModelIds.TAIL_2,
        ModelIds.FRONT_LEFT, ModelIds.FRONT_RIGHT, ModelIds.HIND_LEFT, ModelIds.HIND_RIGHT
    };
    private static final String[] PIECE_NAMES = {
        "head", "jaw", "body_1", "body_2", "body_3", "body_4",
        "tail_1", "tail_2", "front_left", "front_right", "hind_left", "hind_right"
    };

    private ResourcePackBuilder() { }

    static void build(Path output, int packFormat, ClassLoader resources) throws IOException {
        Files.createDirectories(output.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, "pack.mcmeta", "{\"pack\":{\"pack_format\":" + packFormat
                + ",\"description\":\"CatFight models and sounds by moze\"}}\n");
            put(zip, "assets/catfight/sounds.json", soundsJson());
            copy(zip, resources, "pack-sounds/cat_hiss.ogg", "assets/catfight/sounds/cat_hiss.ogg");
            copy(zip, resources, "pack-sounds/cat_tussle.ogg", "assets/catfight/sounds/cat_tussle.ogg");
            put(zip, "assets/minecraft/models/item/carved_pumpkin.json", pumpkinOverrides());

            for (int index = 0; index < PIECE_NAMES.length; index++) {
                put(zip, "assets/catfight/models/item/piece/" + PIECE_NAMES[index] + ".json", pieceModel(PIECE_NAMES[index]));
            }
            put(zip, "assets/catfight/models/item/piece/paper.json", paperModel());

            for (int variant = 0; variant < VARIANTS.length; variant++) {
                String name = VARIANTS[variant];
                for (String piece : PIECE_NAMES) {
                    put(zip, "assets/catfight/models/item/cat/" + name + "/" + piece + ".json",
                        variantModel("catfight:item/piece/" + piece, name, null));
                }
                put(zip, "assets/catfight/models/item/cat/" + name + "/paper.json",
                    variantModel("catfight:item/piece/paper", name, null));
                for (int direction = 0; direction < 16; direction++) {
                    // Keep equivalent display rotations in the vanilla range for 1.20.1 clients.
                    double rotation = direction * 22.5;
                    if (rotation > 180.0) rotation -= 360.0;
                    put(zip, "assets/catfight/models/item/cat/" + name + "/paper_" + direction + ".json",
                        variantModel("catfight:item/cat/" + name + "/paper", name, rotation));
                }
            }
        }
    }

    private static String soundsJson() {
        return "{\n"
            + "  \"cat_hiss\": {\"sounds\": [\"catfight:cat_hiss\"], \"subtitle\": \"Cat hisses\"},\n"
            + "  \"cat_tussle\": {\"sounds\": [\"catfight:cat_tussle\"], \"subtitle\": \"Cats tussle\"}\n"
            + "}\n";
    }

    private static String pumpkinOverrides() {
        List<String> overrides = new ArrayList<>();
        for (int variant = 0; variant < VARIANTS.length; variant++) {
            for (int index = 0; index < PIECES.length; index++) {
                overrides.add(override(ModelIds.piece(variant, PIECES[index]),
                    "catfight:item/cat/" + VARIANTS[variant] + "/" + PIECE_NAMES[index]));
            }
            for (int direction = 0; direction < 16; direction++) {
                overrides.add(override(ModelIds.paper(variant, direction),
                    "catfight:item/cat/" + VARIANTS[variant] + "/paper_" + direction));
            }
        }
        return "{\"parent\":\"minecraft:block/carved_pumpkin\",\"overrides\":["
            + String.join(",", overrides) + "]}\n";
    }

    private static String override(int id, String model) {
        return "{\"predicate\":{\"custom_model_data\":" + id + "},\"model\":\"" + model + "\"}";
    }

    private static String variantModel(String parent, String variant, Double yRotation) {
        String display = yRotation == null ? "" : ",\"display\":{\"head\":{\"rotation\":[0," + yRotation
            + ",0],\"translation\":[0,0,0],\"scale\":[1,1,1]}}";
        return "{\"parent\":\"" + parent + "\",\"textures\":{\"cat\":\"minecraft:entity/cat/"
            + variant + "\"}" + display + "}\n";
    }

    private static String pieceModel(String piece) {
        if (piece.startsWith("body_")) {
            int section = Integer.parseInt(piece.substring(piece.length() - 1)) - 1;
            return bodySection(section);
        }
        if (piece.equals("head")) return box(3, 3, 3, 13, 12, 13, 0, 0, 10, 9);
        if (piece.equals("jaw")) return box(5, 6, 2, 11, 9, 8, 0, 24, 6, 3);
        if (piece.startsWith("tail")) return box(6, 1, 6, 10, 15, 10, 0, 15, 4, 14);
        return box(6, 2, 6, 10, 15, 10, 40, 0, 4, 13);
    }

    private static String bodySection(int section) {
        double v1 = 3.0 + section * 2.0;
        double v2 = v1 + 2.0;
        String faces = "\"north\":{\"uv\":[6.5," + v1 + ",7.5," + v2 + "],\"texture\":\"#cat\"},"
            + "\"south\":{\"uv\":[9," + v1 + ",10," + v2 + "],\"texture\":\"#cat\"},"
            + "\"west\":{\"uv\":[5," + v1 + ",6.5," + v2 + "],\"texture\":\"#cat\"},"
            + "\"east\":{\"uv\":[7.5," + v1 + ",9," + v2 + "],\"texture\":\"#cat\"},"
            + "\"up\":{\"uv\":[7.5,0,8.5,3],\"texture\":\"#cat\"},"
            + "\"down\":{\"uv\":[6.5,0,7.5,3],\"texture\":\"#cat\"}";
        return modelHeader() + "\"elements\":[{\"from\":[5,5,4],\"to\":[11,11,12],\"faces\":{" + faces + "}}]}\n";
    }

    private static String paperModel() {
        String texture = "{\"texture\":\"#cat\",\"uv\":[0,0,16,16]}";
        return modelHeader() + "\"elements\":["
            + flatElement(4, 7.85, 2, 12, 8.15, 13, texture) + ","
            + flatElement(5, 7.84, 12, 11, 8.16, 16, texture) + ","
            + flatElement(2, 7.86, 3, 5, 8.14, 12, texture) + ","
            + flatElement(11, 7.86, 3, 14, 8.14, 12, texture) + ","
            + flatElement(7, 7.87, 0, 9, 8.13, 3, texture) + "]}\n";
    }

    private static String flatElement(double x1, double y1, double z1, double x2, double y2, double z2, String face) {
        return "{\"from\":[" + x1 + "," + y1 + "," + z1 + "],\"to\":[" + x2 + "," + y2 + "," + z2
            + "],\"faces\":{\"up\":" + face + ",\"down\":" + face + "}}";
    }

    private static String box(double x1, double y1, double z1, double x2, double y2, double z2,
                              double u, double v, double width, double height) {
        double u1 = u / 4.0;
        double v1 = v / 2.0;
        double u2 = (u + width) / 4.0;
        double v2 = (v + height) / 2.0;
        String face = "{\"texture\":\"#cat\",\"uv\":[" + u1 + "," + v1 + "," + u2 + "," + v2 + "]}";
        return modelHeader() + "\"elements\":[{\"from\":[" + x1 + "," + y1 + "," + z1 + "],\"to\":["
            + x2 + "," + y2 + "," + z2 + "],\"faces\":{\"north\":" + face + ",\"south\":" + face
            + ",\"west\":" + face + ",\"east\":" + face + ",\"up\":" + face + ",\"down\":" + face + "}}]}\n";
    }

    private static String modelHeader() {
        return "{\"credit\":\"moze CatFight\",\"textures\":{\"particle\":\"#cat\"},"
            + "\"display\":{\"head\":{\"rotation\":[0,0,0],\"translation\":[0,0,0],\"scale\":[0.55,0.55,0.55]}},";
    }

    private static void put(ZipOutputStream zip, String path, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void copy(ZipOutputStream zip, ClassLoader resources, String source, String target) throws IOException {
        try (InputStream input = resources.getResourceAsStream(source)) {
            if (input == null) throw new IOException("Missing bundled resource " + source);
            zip.putNextEntry(new ZipEntry(target));
            input.transferTo(zip);
            zip.closeEntry();
        }
    }
}
