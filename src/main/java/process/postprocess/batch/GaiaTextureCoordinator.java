package process.postprocess.batch;

import basic.geometry.GaiaRectangle;
import basic.exchangable.GaiaBuffer;
import basic.exchangable.GaiaBufferDataSet;
import basic.structure.GaiaMaterial;
import basic.structure.GaiaTexture;
import basic.types.AttributeType;
import basic.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Vector2d;
import process.tileprocess.tile.LevelOfDetail;
import util.ImageResizer;
import util.ImageUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

@Slf4j
public class GaiaTextureCoordinator {
    final private String ATLAS_IMAGE;
    final private double ERROR = 10E-5; //error = 10E-5;

    private final List<GaiaMaterial> materials;
    private final List<GaiaBufferDataSet> bufferDataSets;
    private BufferedImage atlasImage;

    public GaiaTextureCoordinator(String name, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.ATLAS_IMAGE = name;
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.initBatchImage(0, 0);
    }
    private void initBatchImage(int width, int height) {
        if (width > 0 || height > 0) {
            this.atlasImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        } else {
            this.atlasImage = null;
        }
    }
    public void writeBatchedImage() {
        File file = new File("D:\\MAGO_TEST_FOLDER\\ComplicatedModels\\output\\images\\");
        file.mkdir();
        Path outputPath = file.toPath();
        Path output = file.toPath().resolve(ATLAS_IMAGE + ".jpg");
        if (!outputPath.toFile().exists()) {
            outputPath.toFile().mkdir();
        }
        if (this.atlasImage != null) {
            try {
                ImageIO.write(this.atlasImage, "jpg", output.toFile());
                ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.0f);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    private boolean intersectsRectangle_atlasingProcess(List<GaiaRectangle>listRectangles, GaiaRectangle rectangle)
    {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles.***
        boolean intersects = false;
        double error = 10E-5;
        int listRectanglesCount = listRectangles.size();
        for(int i=0; i<listRectanglesCount; i++)
        {
            GaiaRectangle existentRectangle = listRectangles.get(i);
            if(existentRectangle == rectangle)
            {
                continue;
            }

            if(existentRectangle.intersects(rectangle, error))
            {
                intersects = true;
                break;
            }
        }
        return intersects;
    }

    private Vector2d getBestPositionMosaicInAtlas(List<GaiaBatchImage>listProcessSplitDatas, GaiaBatchImage splitData_toPutInMosaic)
    {
        Vector2d resultVec = new Vector2d();

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;

        GaiaRectangle rect_toPutInMosaic = splitData_toPutInMosaic.getOriginBoundary();

        // make existent rectangles list using listProcessSplitDatas.***
        List<GaiaRectangle> list_rectangles = new ArrayList<>();
        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        int existentSplitDatasCount = listProcessSplitDatas.size();
        for(int i=0; i<existentSplitDatasCount; i++)
        {
            GaiaBatchImage existentSplitData = listProcessSplitDatas.get(i);
            GaiaRectangle batchedBoundary = existentSplitData.batchedBoundary;
            if(i==0)
            {
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            }
            else
            {
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }
            list_rectangles.add(batchedBoundary);
        }

        // Now, try to find the best positions to put our rectangle.***
        for(int i=0; i<existentSplitDatasCount; i++)
        {
            GaiaBatchImage existentSplitData = listProcessSplitDatas.get(i);
            GaiaRectangle currRect = existentSplitData.batchedBoundary;

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown.***
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitData_toPutInMosaic".***

            // If in some of two positions our rectangle intersects with any other rectangle, then discard.***
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic.***

            double width = splitData_toPutInMosaic.getOriginBoundary().getWidth();
            double height = splitData_toPutInMosaic.getOriginBoundary().getHeight();

            // 1- leftUp corner.***
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle.***
            if(splitData_toPutInMosaic.batchedBoundary == null)
            {
                splitData_toPutInMosaic.batchedBoundary = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
            }
            splitData_toPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitData_toPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitData_toPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitData_toPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if(!this.intersectsRectangle_atlasingProcess(list_rectangles, splitData_toPutInMosaic.batchedBoundary))
            {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitData_toPutInMosaic.batchedBoundary);

                // calculate the perimeter of the mosaic.***
                if(candidateMosaicPerimeter < 0.0)
                {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                }
                else
                {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter > currMosaicPerimeter) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                    }
                }
            }

            // 2- rightDown corner.***
            currPosX = currRect.getMaxX();
            currPosY = currRect.getMinY();

            // setup our rectangle.***
            splitData_toPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitData_toPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitData_toPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitData_toPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if(!this.intersectsRectangle_atlasingProcess(list_rectangles, splitData_toPutInMosaic.batchedBoundary))
            {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitData_toPutInMosaic.batchedBoundary);

                // calculate the perimeter of the mosaic.***
                if(candidateMosaicPerimeter < 0.0)
                {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                }
                else
                {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter > currMosaicPerimeter) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                    }
                }
            }
        }

        resultVec.set(candidatePosX, candidatePosY);

        return resultVec;
    }

    private float modf(float value, Double intPart)
    {
        intPart = Math.floor(value);
        return (float)(value - intPart);
    }

    public void batchTextures(LevelOfDetail lod, CommandLine command) {
        // We have MaterialList & BufferDataSetList.********
        // 1- List<GaiaMaterial> this.materials;
        // 2- List<GaiaBufferDataSet> this.bufferDataSets;

        // The atlasImage is the final image.********
        // BufferedImage this.atlasImage;
        //--------------------------------------------------------

        // 1rst, make a list of GaiaBatchImage (splittedImage).********
        List<GaiaBatchImage> splittedImages = new ArrayList<>();
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = null;
            BufferedImage bufferedImage;
            if (textures.size() > 0) {
                texture = textures.get(0);
                bufferedImage = texture.getBufferedImage(lod.getTextureScale());
            } else {
                bufferedImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = bufferedImage.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, 32, 32);
                graphics.dispose();
            }

            //BufferedImage bufferedImage = texture.getBufferedImage(lod.getTextureScale());
            //bufferedImage = resizeImage(bufferedImage, lod);
            //texture.setBufferedImage(bufferedImage);

            Vector2d minPoint = new Vector2d(0, 0);
            Vector2d maxPoint = new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight());

            GaiaBatchImage splittedImage = new GaiaBatchImage();
            splittedImage.setOriginBoundary(new GaiaRectangle(minPoint, maxPoint));
            splittedImage.setMaterialId(material.getId());
            splittedImages.add(splittedImage);
        }

        // 사이즈 큰->작은 정렬
        splittedImages = splittedImages.stream()
                .sorted(Comparator.comparing(splittedImage -> splittedImage.getOriginBoundary().getArea()))
                .collect(Collectors.toList());
        Collections.reverse(splittedImages);

        // do the atlasing process.***
        List<GaiaBatchImage>listProcessSplitDatas = new ArrayList<>();
        for(int i=0; i<splittedImages.size(); i++)
        {
            GaiaBatchImage splittedImage = splittedImages.get(i);
            GaiaRectangle originBoundary = splittedImage.getOriginBoundary();

            if(i==0)
            {
                splittedImage.setBatchedBoundary(originBoundary);
            }
            else
            {
                // 1rst, find the best position for image into atlas.***
                Vector2d bestPosition = this.getBestPositionMosaicInAtlas(listProcessSplitDatas, splittedImage);
                splittedImage.batchedBoundary.setMinX(bestPosition.x);
                splittedImage.batchedBoundary.setMinY(bestPosition.y);
                splittedImage.batchedBoundary.setMaxX(bestPosition.x + originBoundary.getWidth());
                splittedImage.batchedBoundary.setMaxY(bestPosition.y + originBoundary.getHeight());

            }

            listProcessSplitDatas.add(splittedImage);
        }

        /*
        // 분할이미지
        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle targetRectangle = target.getOriginBoundary();
            List<GaiaBatchImage> compareImages = getListWithoutSelf(target, splittedImages);

            if (compareImages.isEmpty()) {
                target.setBatchedBoundary(targetRectangle);
            } else {
                List<GaiaBatchImage> leftBottomFiltered = compareImages.stream().filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                            List<GaiaBatchImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaBatchImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getBatchedBoundary();
                                if (compareRectangle.intersects(leftBottom, ERROR)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);

                            int maxWidth = getMaxWidth(compareImages);
                            int maxHeight = getMaxHeight(compareImages);
                            maxWidth = maxWidth < leftBottom.getMaxX() ? (int) leftBottom.getMaxX() : maxWidth;
                            maxHeight = maxHeight < leftBottom.getMaxY() ? (int) leftBottom.getMaxY() : maxHeight;
                            return maxHeight * maxWidth;
                        })).collect(Collectors.toList());

                List<GaiaBatchImage> rightTopFiltered = compareImages.stream()
                        .filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                            List<GaiaBatchImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaBatchImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getBatchedBoundary();
                                if (compareRectangle.intersects(rightTop, ERROR)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                            int maxWidth = getMaxWidth(compareImages);
                            int maxHeight = getMaxHeight(compareImages);
                            maxWidth = maxWidth < rightTop.getMaxX() ? (int) rightTop.getMaxX() : maxWidth;
                            maxHeight = maxHeight < rightTop.getMaxY() ? (int) rightTop.getMaxY() : maxHeight;
                            return maxHeight * maxWidth;
                        })).collect(Collectors.toList());

                if (!leftBottomFiltered.isEmpty() && !rightTopFiltered.isEmpty()) {
                    GaiaBatchImage leftBottomImage = leftBottomFiltered.get(0);
                    GaiaRectangle leftBottomCompare = leftBottomImage.getBatchedBoundary();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, leftBottomCompare);

                    GaiaBatchImage rightTopImage = rightTopFiltered.get(0);
                    GaiaRectangle rightTopCompare = rightTopImage.getBatchedBoundary();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, rightTopCompare);

                    if (leftBottom.getBoundingArea() >= rightTop.getBoundingArea()) {
                        target.setBatchedBoundary(rightTop);
                    } else {
                        target.setBatchedBoundary(leftBottom);
                    }
                } else if (!leftBottomFiltered.isEmpty()) {
                    GaiaBatchImage notCompareImage = leftBottomFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getBatchedBoundary();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                    target.setBatchedBoundary(leftBottom);
                } else if (!rightTopFiltered.isEmpty()) {
                    GaiaBatchImage notCompareImage = rightTopFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getBatchedBoundary();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                    target.setBatchedBoundary(rightTop);
                }
            }
        }

         */

        int maxWidth = getMaxWidth(splittedImages);
        int maxHeight = getMaxHeight(splittedImages);
        initBatchImage(maxWidth, maxHeight);
        if (this.atlasImage == null) {
            log.error("atlasImage is null");
            return;
        }

        Graphics graphics = this.atlasImage.getGraphics();

        for (GaiaBatchImage splittedImage : splittedImages) {
            GaiaRectangle splittedRectangle = splittedImage.getBatchedBoundary();
            GaiaMaterial material = findMaterial(splittedImage.getMaterialId());

            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            if (textures.size() > 0) {
                GaiaTexture texture = textures.get(0);
                BufferedImage source = texture.getBufferedImage();
                graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(),null);
            }
        }

        if (command != null && command.hasOption("debug")) {
            float[] debugColor = lod.getDebugColor();
            Color color = new Color(debugColor[0], debugColor[1], debugColor[2], 0.5f);
            graphics.setColor(color);
            graphics.fillRect(0, 0, maxWidth, maxHeight);
        }

        //this.atlasImage = resizeNearestPowerOfTwo(this.atlasImage);

        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle splittedRectangle = target.getBatchedBoundary();

            int width = (int) splittedRectangle.getMaxX() - (int) splittedRectangle.getMinX();
            int height = (int) splittedRectangle.getMaxY() - (int) splittedRectangle.getMinY();

            GaiaMaterial material = findMaterial(target.getMaterialId());
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);

            GaiaTexture texture = null;
            if (textures.size() > 0) {
                texture = textures.get(0);
            } else {
                texture = new GaiaTexture();
                texture.setType(TextureType.DIFFUSE);
                textures.add(texture);
            }

            texture.setBufferedImage(this.atlasImage);
            texture.setWidth(maxWidth);
            texture.setHeight(maxHeight);
            texture.setPath(ATLAS_IMAGE + ".jpg");

            List<GaiaBufferDataSet> materialBufferDataSets = bufferDataSets.stream()
                    .filter((bufferDataSet) -> bufferDataSet.getMaterialId() == target.getMaterialId())
                    .collect(Collectors.toList());

            Double intPart_x = null, intPart_y = null;
            double fractPart_x, fractPart_y;
            double error = 1e-8;
            for (GaiaBufferDataSet materialBufferDataSet : materialBufferDataSets) {
                GaiaBuffer texcoordBuffer = materialBufferDataSet.getBuffers().get(AttributeType.TEXCOORD);
                if (texcoordBuffer != null) {
                    float[] texcoords = texcoordBuffer.getFloats();
                    for (int i = 0; i < texcoords.length; i+=2) {
                        float originX = texcoords[i];
                        float originY = texcoords[i + 1];

                        double u, v;
                        double u2, v2;

                        if(abs(originX) - 1.0 < error)
                        {
                            fractPart_x = originX;
                        }
                        else {
                            fractPart_x = this.modf(originX, intPart_x);
                        }

                        if(abs(originY) - 1.0 < error)
                        {
                            fractPart_y = originY;
                        }
                        else {
                            fractPart_y = this.modf(originY, intPart_y);
                        }

                        u = fractPart_x;
                        v = fractPart_y;

                        if(u<0.0)
                        {
                            u = 1.0 + u;
                        }

                        u2 =  (splittedRectangle.getMinX() + u * width) / maxWidth;
                        v2 =  (splittedRectangle.getMinY() + v * height) / maxHeight;
                        /*
                        float convertX = originX * width;
                        float convertY = originY * height;
                        float offsetX = (float) (splittedRectangle.getMinX() + convertX);
                        float offsetY = (float) (splittedRectangle.getMinY() + convertY);
                        float resultX = offsetX / maxWidth;
                        float resultY = offsetY / maxHeight;
                        texcoords[i] = resultX;
                        texcoords[i + 1] = resultY;

                         */

                        texcoords[i] = (float) (u2);
                        texcoords[i + 1] = (float) (v2);
                    }
                }

            }
        }
        //writeBatchedImage();
        //this.atlasImage = null;
    }

    /*private BufferedImage resizeNearestPowerOfTwo(BufferedImage bufferedImage) {
        ImageResizer resizer = new ImageResizer();
        int resizeWidth = ImageUtils.getNearestPowerOfTwo(bufferedImage.getWidth());
        int resizeHeight = ImageUtils.getNearestPowerOfTwo(bufferedImage.getHeight());
        bufferedImage = resizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
        return bufferedImage;
    }*/

    //findMaterial
    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream()
                .filter(material -> material.getId() == materialId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found material"));
    }

    private List<GaiaBatchImage> getListWithoutSelf(GaiaBatchImage targetSplittedImage, List<GaiaBatchImage> splittedImages) {
        return splittedImages.stream()
                .filter(splittedImage -> (splittedImage != targetSplittedImage) && (splittedImage.getBatchedBoundary() != null))
                .collect(Collectors.toList());
    }

    private GaiaRectangle getRightTop(GaiaRectangle target, GaiaRectangle compare) {
        Vector2d rightTopPoint = compare.getRightTopPoint();
        Vector2d rightTopMaxPoint = new Vector2d(rightTopPoint.x + target.getMaxX(), rightTopPoint.y + target.getMaxY());
        GaiaRectangle rightTopRectangle = new GaiaRectangle();
        rightTopRectangle.setInit(rightTopPoint);
        rightTopRectangle.addPoint(rightTopMaxPoint);
        return rightTopRectangle;
    }
    private GaiaRectangle getLeftBottom(GaiaRectangle target, GaiaRectangle compare) {
        Vector2d leftBottomPoint = compare.getLeftBottomPoint();
        Vector2d leftBottomMaxPoint = new Vector2d(leftBottomPoint.x + target.getMaxX(), leftBottomPoint.y + target.getMaxY());
        GaiaRectangle leftBottomRectangle = new GaiaRectangle();
        leftBottomRectangle.setInit(leftBottomPoint);
        leftBottomRectangle.addPoint(leftBottomMaxPoint);
        return leftBottomRectangle;
    }

    private int getMaxWidth(List<GaiaBatchImage> compareImages) {
        return compareImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxX())
                .max()
                .orElse(0);
    }

    private int getMaxHeight(List<GaiaBatchImage> compareImages) {
        return compareImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxY())
                .max()
                .orElse(0);
    }
}


