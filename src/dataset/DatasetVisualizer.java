package dataset;

import index.DataPoint;
import index.RangeDataset;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static dataset.ReadDataset.readDataset;
import static dataset.ReadDataset.readWorkload;

public class DatasetVisualizer extends JPanel {
    static HashSet<DataPoint[]> rectangles;

    static HashSet<DataPoint[]> partitions;

    private static RangeDataset dataset;
    private int panelSize = 1000;
    private static int[] minValues;
    private static int[] maxValues;
    private int[] XsplitPoints = new int[1];
    private int[] YsplitPoints = new int[1];

    public DatasetVisualizer(HashSet<DataPoint[]> rectangles, RangeDataset dataset, int[] minValues, int[] maxValues) {
        this.rectangles = rectangles;
        this.minValues = minValues;
        this.maxValues = maxValues;
        this.dataset = dataset;
    }

    public DatasetVisualizer(HashSet<DataPoint[]> rectangles, RangeDataset dataset, HashSet<DataPoint[]> partitions, int[] minValues, int[] maxValues) {
        this.rectangles = rectangles;
        this.minValues = minValues;
        this.maxValues = maxValues;
        this.dataset = dataset;
        this.partitions = partitions;
    }

    public DatasetVisualizer(HashSet<DataPoint[]> rectangles, RangeDataset dataset, int[] minValues, int[] maxValues, int dimension, int splitPoint) {
        this.rectangles = rectangles;
        this.minValues = minValues;
        this.maxValues = maxValues;
        this.dataset = dataset;
        if (dimension == 0) {XsplitPoints[0] = splitPoint;}
        if (dimension == 1) {YsplitPoints[0] = splitPoint;}
    }


    // 独立绘制方法（同时支持屏幕绘制和文件导出）
    private static void drawAllElements(Graphics2D g2d, int canvasSize,
                                 int[] minValues, int[] maxValues) {
        // 计算缩放比例
        double scaleX = canvasSize / (double) (maxValues[0] - minValues[0]);
        double scaleY = canvasSize / (double) (maxValues[1] - minValues[1]);

        // 计算居中偏移量
        int offsetX = (int) ((canvasSize - (maxValues[0] - minValues[0]) * scaleX) / 2);
        int offsetY = (int) ((canvasSize - (maxValues[1] - minValues[1]) * scaleY) / 2);

//        // 绘制 partitions（保持原有逻辑）
//        g2d.setColor(Color.BLACK);
//        g2d.setStroke(new BasicStroke(1f));
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//        for (DataPoint[] rect : partitions) {
//            double x = (rect[0].data[0] - minValues[0]) * scaleX + offsetX;
//            double y = (rect[0].data[1] - minValues[1]) * scaleY + offsetY;
//            double width = (rect[1].data[0] - rect[0].data[0]) * scaleX;
//            double height = (rect[1].data[1] - rect[0].data[1]) * scaleY;
//            g2d.draw(new Rectangle2D.Double(x, canvasSize - y - height, width, height));
//        }

        // 绘制 query workload
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1f));
        for (DataPoint[] rect : rectangles) {
            int x = (int) ((rect[0].data[0] - minValues[0]) * scaleX) + offsetX;
            int y = (int) ((rect[0].data[1] - minValues[1]) * scaleY) + offsetY;
            int width = (int) ((rect[1].data[0] - rect[0].data[0]) * scaleX);
            int height = (int) ((rect[1].data[1] - rect[0].data[1]) * scaleY);
            g2d.drawRect(x, canvasSize - y - height, width, height);
        }

        // 绘制数据点
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(0.5f));
        for (DataPoint point : dataset.dataset) {
            int x = (int) ((point.data[0] - minValues[0]) * scaleX) + offsetX;
            int y = (int) ((point.data[1] - minValues[1]) * scaleY) + offsetY;
            g2d.fillOval(x - 1, canvasSize - y - 1, 2, 2);
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 设置背景色（确保与导出一致）
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 调用通用绘制方法（使用屏幕尺寸）
        drawAllElements(g2d, panelSize, minValues, maxValues);
    }

    public static void exportHighResImage(String filePath, int exportSize) {
        // 创建 BufferedImage
        BufferedImage image = new BufferedImage(exportSize, exportSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D exportG2d = image.createGraphics();

        // 设置白色背景
        exportG2d.setColor(Color.WHITE);
        exportG2d.fillRect(0, 0, exportSize, exportSize);

        // 调用通用绘制方法（使用导出尺寸）
        drawAllElements(exportG2d, exportSize, minValues, maxValues);

        // 释放资源
        exportG2d.dispose();

        // 将 BufferedImage 转换为 PDF
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(exportSize, exportSize));
            document.addPage(page);

            // 使用 JPEGFactory 创建 PDImageXObject
            PDImageXObject pdImage = JPEGFactory.createFromImage(document, image);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, exportSize, exportSize);
            }

            // 保存 PDF 文件
            document.save(filePath);
            System.out.println("成功导出 PDF 到: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void exportHighResImage(String filePath, int exportSize) {
//        BufferedImage image = new BufferedImage(exportSize, exportSize, BufferedImage.TYPE_INT_RGB);
//        Graphics2D exportG2d = image.createGraphics();
//
//        // 设置白色背景
//        exportG2d.setColor(Color.WHITE);
//        exportG2d.fillRect(0, 0, exportSize, exportSize);
//
//        // 调用通用绘制方法（使用导出尺寸）
//        drawAllElements(exportG2d, exportSize, minValues, maxValues);
//
//        // 保存文件
//        try {
//            ImageIO.write(image, "PDF", new File(filePath));
//            System.out.println("成功导出图像到: " + filePath);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            exportG2d.dispose();
//        }
//    }



    public static void draw(HashSet<DataPoint[]> rectangles, List<DataPoint> datasets, HashSet<DataPoint[]> partitions) {
        JFrame frame = new JFrame("2D Rectangle Visualization");
        RangeDataset rangeDataset = new RangeDataset(datasets);

        for (DataPoint[] rect : rectangles) {
            rangeDataset.range[0].data[0] = Math.min(rangeDataset.range[0].data[0], rect[0].data[0]);
            rangeDataset.range[0].data[1] = Math.min(rangeDataset.range[0].data[1], rect[0].data[1]);
            rangeDataset.range[1].data[0] = Math.max(rangeDataset.range[1].data[0], rect[1].data[0]);
            rangeDataset.range[1].data[1] = Math.max(rangeDataset.range[1].data[1], rect[1].data[1]);
        }

        DatasetVisualizer panel = new DatasetVisualizer(rectangles, rangeDataset, partitions, rangeDataset.range[0].data, rangeDataset.range[1].data);
        panel.setPreferredSize(new Dimension(1100, 1100));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        exportHighResImage("./drawTest.pdf", 1024);
    }

    public static void main(String[] args) {
//        RangeDataset dataset = readDataset("./test.txt");
        List<DataPoint> dataPoints = readDataset("D:\\paper_source\\work_6\\dataset\\OSM_1M");
        dataPoints = dataPoints.subList(80000, 90000);
        RangeDataset dataset = new RangeDataset(dataPoints);
        HashSet<DataPoint[]> rectangles = readWorkload("D:\\paper_source\\work_6\\dataset\\MIX_Workload_OSM_1M_R0.002%", 800);
        JFrame frame = new JFrame("2D Rectangle Visualization");
        DatasetVisualizer panel = new DatasetVisualizer(rectangles, dataset, dataset.range[0].data, dataset.range[1].data);
        panel.setPreferredSize(new Dimension(1000, 1000));
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
