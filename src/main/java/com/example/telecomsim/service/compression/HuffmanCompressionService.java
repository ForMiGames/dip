package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanCompressionService  extends AbstractCompressionService {
    // Размер таблицы частот в заголовке: 256 символов × 4 байта (int)
    private static final int FREQUENCY_TABLE_SIZE = 256;
    private static final int HEADER_BYTES = FREQUENCY_TABLE_SIZE * Integer.BYTES + Integer.BYTES;
    // +4 байта для хранения количества значащих бит в последнем байте

    // ------------------------------------------------------------------ //
    //  Открытый API                                                        //
    // ------------------------------------------------------------------ //

    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.HUFFMAN;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");

        int[] frequencies = buildFrequencyTable(data);
        HuffmanNode root = buildHuffmanTree(frequencies);
        String[] codes = buildCodeTable(root);

        // Сериализуем биткод
        BitOutputStream bitStream = new BitOutputStream();
        for (byte b : data) {
            String code = codes[b & 0xFF];
            for (char bit : code.toCharArray()) {
                bitStream.writeBit(bit == '1');
            }
        }
        byte[] encodedBits = bitStream.toByteArray();
        int validBitsInLastByte = bitStream.getValidBitsInLastByte();

        // Формируем выходной массив: [таблица частот][validBits][данные]
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + encodedBits.length);
        for (int freq : frequencies) {
            buffer.putInt(freq);
        }
        buffer.putInt(validBitsInLastByte);
        buffer.put(encodedBits);

        return buffer.array();
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");

        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Читаем таблицу частот и восстанавливаем дерево
        int[] frequencies = new int[FREQUENCY_TABLE_SIZE];
        for (int i = 0; i < FREQUENCY_TABLE_SIZE; i++) {
            frequencies[i] = buffer.getInt();
        }
        int validBitsInLastByte = buffer.getInt();

        HuffmanNode root = buildHuffmanTree(frequencies);

        // Читаем закодированные байты
        byte[] encodedBits = new byte[buffer.remaining()];
        buffer.get(encodedBits);

        // Подсчитываем общее количество значащих бит
        long totalBits = (long) (encodedBits.length - 1) * 8 + validBitsInLastByte;

        // Декодируем, обходя дерево
        int totalSymbols = countTotalSymbols(frequencies);
        ByteArrayOutputStream result = new ByteArrayOutputStream(totalSymbols);
        HuffmanNode current = root;
        long bitsRead = 0;

        for (int byteIndex = 0; byteIndex < encodedBits.length && bitsRead < totalBits; byteIndex++) {
            int bitsInByte = (byteIndex == encodedBits.length - 1) ? validBitsInLastByte : 8;
            for (int bit = 7; bit >= 8 - bitsInByte && bitsRead < totalBits; bit--) {
                boolean isOne = ((encodedBits[byteIndex] >> bit) & 1) == 1;
                current = isOne ? current.right : current.left;

                if (current == null) {
                    throw new CompressionException(
                            getAlgorithm(), "Повреждённые данные: некорректный путь в дереве Хаффмана"
                    );
                }

                if (current.isLeaf()) {
                    result.write(current.symbol);
                    current = root;
                }
                bitsRead++;
            }
        }

        return result.toByteArray();
    }

    @Override
    public String getDescription() {
        return """
               Алгоритм Хаффмана — классический метод энтропийного кодирования без потерь.
               Часто встречающимся символам назначаются короткие бинарные коды, редким — длинные.
               Обеспечивает сжатие, близкое к теоретическому пределу Шеннона.
               Эффективен для текстовых данных с неравномерным распределением символов.
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(); // Классический Хаффман не имеет настраиваемых параметров
    }

    // ------------------------------------------------------------------ //
    //  Приватные методы                                                    //
    // ------------------------------------------------------------------ //

    private int[] buildFrequencyTable(byte[] data) {
        int[] freq = new int[FREQUENCY_TABLE_SIZE];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        return freq;
    }

    private HuffmanNode buildHuffmanTree(int[] frequencies) {
        // PriorityQueue — минимальная куча по частоте
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.frequency)
        );
        for (int i = 0; i < FREQUENCY_TABLE_SIZE; i++) {
            if (frequencies[i] > 0) {
                queue.add(new HuffmanNode(i, frequencies[i]));
            }
        }

        // Крайний случай: все данные — один уникальный символ
        if (queue.size() == 1) {
            HuffmanNode only = queue.poll();
            queue.add(new HuffmanNode(-1, 0, only, null));
        }

        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            queue.add(new HuffmanNode(-1, left.frequency + right.frequency, left, right));
        }

        return queue.poll();
    }

    private String[] buildCodeTable(HuffmanNode root) {
        String[] codes = new String[FREQUENCY_TABLE_SIZE];
        buildCodesRecursive(root, "", codes);
        return codes;
    }

    private void buildCodesRecursive(HuffmanNode node, String prefix, String[] codes) {
        if (node == null) return;
        if (node.isLeaf()) {
            // Если дерево из одного узла — назначаем код "0"
            codes[node.symbol] = prefix.isEmpty() ? "0" : prefix;
            return;
        }
        buildCodesRecursive(node.left,  prefix + "0", codes);
        buildCodesRecursive(node.right, prefix + "1", codes);
    }

    private int countTotalSymbols(int[] frequencies) {
        int total = 0;
        for (int f : frequencies) total += f;
        return total;
    }

    // ------------------------------------------------------------------ //
    //  Внутренние вспомогательные классы                                  //
    // ------------------------------------------------------------------ //

    private static class HuffmanNode {
        final int symbol;        // -1 для внутренних узлов
        final int frequency;
        final HuffmanNode left;
        final HuffmanNode right;

        // Листовой узел
        HuffmanNode(int symbol, int frequency) {
            this(symbol, frequency, null, null);
        }

        // Внутренний узел
        HuffmanNode(int symbol, int frequency, HuffmanNode left, HuffmanNode right) {
            this.symbol    = symbol;
            this.frequency = frequency;
            this.left      = left;
            this.right     = right;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    /**
     * Буфер для побитовой записи, упаковывающий биты в байты.
     */
    private static class BitOutputStream {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private int currentByte = 0;
        private int bitCount = 0;   // Бит записано в текущий байт

        void writeBit(boolean one) {
            if (one) {
                currentByte |= (1 << (7 - bitCount));
            }
            bitCount++;
            if (bitCount == 8) {
                bytes.write(currentByte);
                currentByte = 0;
                bitCount = 0;
            }
        }

        byte[] toByteArray() {
            if (bitCount > 0) {
                bytes.write(currentByte); // Дозаписываем последний неполный байт
            }
            return bytes.toByteArray();
        }

        /** Сколько значащих бит в последнем байте (1–8). */
        int getValidBitsInLastByte() {
            return bitCount == 0 ? 8 : bitCount;
        }
    }
}
