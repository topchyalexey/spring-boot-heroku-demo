package ru.lotetop.qrdocs.acr.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.detector.MultiDetector;
import com.google.zxing.qrcode.QRCodeReader;

public class ImageUtil {

	public static BufferedImage toBufferedImage(BitMatrix bitMatrix) {
		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
   
	public static BitMatrix toBitMatrixBlack(BufferedImage bufferedImage) {
		try {
			return toBinalizer(bufferedImage).getBlackMatrix();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static BitMatrix toBitMatrix(InputStream stream) {
		try {
			return toBitMatrixBlack(ImageIO.read(stream));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}			
	}

	public static String getBarcodeData(BufferedImage image) throws NotFoundException, FormatException, ChecksumException {
		return getBarcodeData(image, 100);
	}
	
	public static Result geBarcodeImpl(BufferedImage image, int scalePercents) throws NotFoundException,FormatException,ChecksumException {
	    BinaryBitmap bitmap = new BinaryBitmap(toBinalizer( scaleByProportion(image, new Float(scalePercents)/100)));
		try {
			QRCodeReader reader = new QRCodeReader();
			Result result = reader.decode(bitmap, getQrDetectionHints());
			return new Result( result.getText(), result.getRawBytes(), 
						adjustPointsToScale(result.getResultPoints(), scalePercents), result.getBarcodeFormat());
		} catch(NotFoundException|FormatException|ChecksumException e) {
			//writeDebug(toBufferedImage(bitmap.getBlackMatrix()));
			try {
				return new QRCodeReader().decode(bitmap);
			} catch(NotFoundException|FormatException|ChecksumException ex) {
				if ( scalePercents < 10) {
					throw e;
				}
				return geBarcodeImpl(image, scalePercents/2);
			}
		}
	}
	
	public static Result getBarcode(BufferedImage image) throws NotFoundException,FormatException,ChecksumException {
		return geBarcodeImpl(image, 100);		
	}
	
	public static Result getBarcode3(BufferedImage img) {
		BufferedImage[] images = splitTo4Parts(img);
		// writeDebug(images[0]);
		// writeDebug(images[1]);
		// writeDebug(images[2]);
		// writeDebug(images[3]);
		String operation = " detect Left Bottom QRCode";
		try {
			ResultPoint leftBottom = getBarcode(images[2]).getResultPoints()[0];
			operation = " detect Left Top QRCode";
			Result bcLeftTop = getBarcode(images[0]);
			ResultPoint leftTop = bcLeftTop.getResultPoints()[1];
			operation = " detect Right Top QRCode";
			ResultPoint rightTop = getBarcode(images[1]).getResultPoints()[2];
			int halfW = (int) (0.5*img.getWidth());
			int halfH = (int) (0.5*img.getHeight());
			return new Result(bcLeftTop.getText(), bcLeftTop.getRawBytes(), new ResultPoint[] {
					new ResultPoint(leftBottom.getX(), leftBottom.getY() + halfH),
					leftTop,
					new ResultPoint(rightTop.getX() + halfW, rightTop.getY()),
			}, bcLeftTop.getBarcodeFormat());
		} catch (NotFoundException| FormatException| ChecksumException e) {
			
			throw new RuntimeException("Fail to" + operation);
		}
	}

	private static BufferedImage[] splitTo4Parts(BufferedImage img) {
		int w = (int) (0.5*img.getWidth());
		int h = (int) (0.5*img.getHeight());
		return new BufferedImage[] { 
				img.getSubimage(0, 0, w, h), 
				img.getSubimage(w, 0, w, h),
				
				img.getSubimage(0, h, w, h),
				img.getSubimage(w, h, w, h)};
	}

	/*
	public static ResultPoint[] detectBarcode(BufferedImage image, int scalePercents) throws NotFoundException,FormatException {
		try {
			BitMatrix bm = toBitMatrixBlack(scaleByProportion(image, new Float(scalePercents)/100));			
			ResultPoint[] detectBarcode = detectBarcode(bm);
			return adjustPointsToScale(detectBarcode, scalePercents);
		} catch(NotFoundException|FormatException e) {
			if ( scalePercents >= 50) {
				throw e;
			}
			return detectBarcode(image, scalePercents*2);
		}
	} */
	
	public static String getBarcodeData(BufferedImage image, int scalePercents) throws NotFoundException,FormatException,ChecksumException {
		return getBarcode(image).getText();
	}	

	public static ResultPoint[] detectBarcode( BitMatrix  imgBitMatrixObj) throws NotFoundException, FormatException  {
		MultiDetector detector = new MultiDetector(imgBitMatrixObj);
		Map<DecodeHintType, Object> hints = getQrDetectionHints();		
		DetectorResult dResult = detector.detect(hints);
		return dResult.getPoints();
	}
	
	public static ResultPoint[] detectBarcode(BufferedImage image) throws NotFoundException, FormatException, ChecksumException {
		return getBarcode(image).getResultPoints();
	}
	
	static void writeDebug(BufferedImage i, String name) {
		String fileName = "temp/" +UUID.randomUUID().toString().substring(30) +"_" + name + ".png";
		System.err.println(fileName);
		writeTo(i, fileName);
	}
	
	static void writeDebug(BufferedImage i) {
		writeDebug(i, "");
	}
	
	public static void writeTo(Image image, String fileName) {
		try {
			ImageIO.write(toBufferedImage(image), "png", new File(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean recognizeCheckBoxChecked(BufferedImage image) {
		BitMatrix bitMatrix = toBitMatrixBlack(image);
		//bitMatrix = removeFrame(bitMatrix);
		int blackPixelsPercents = getBlackPixelsPercents(bitMatrix);
		return blackPixelsPercents >= 3;
	}
	
	public static int getBlackPixelsPercents(BitMatrix bitMatrix) {
		
		int blackCount = 0;
		int total = bitMatrix.getHeight()*bitMatrix.getWidth();		
		
		for (int x = 0; x < bitMatrix.getHeight(); x++) {
			for (int y= 0; y < bitMatrix.getWidth(); y++) {
				boolean pixelBlack = bitMatrix.get(y, x);
				if (pixelBlack) {
					++blackCount;
				}
			}
		} // (pixel & 0x00FFFFFF) == 0
		if ( blackCount == 0 || total == 0) {
			return 0;
		}
		return 100/(total/blackCount);
	}

	public static ResultPoint[] adjustPointsToScale(ResultPoint[] points, int scalePercents) {
		if ( scalePercents > 99 ) {
			return points;
		}
		List<ResultPoint> result = new ArrayList<ResultPoint>(points.length);
		float scalePrportion = new Float(100)/scalePercents;
		for (ResultPoint point : points) {
			result.add(new ResultPoint(point.getX() * scalePrportion , point.getY() * scalePrportion));
		}
		return result.toArray(new ResultPoint[] {});
	}
	
	private static Map<DecodeHintType, Object> getQrDetectionHints() {
		Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
		// hints.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
		return hints;
	}

	public static HybridBinarizer normalizeImage(BufferedImage image) {
		BufferedImage bufferedImage = adjustContrastAndBrigtness(image);
		HybridBinarizer hb = toBinalizer(bufferedImage);
		return hb;
	}

	private static HybridBinarizer toBinalizer(BufferedImage bufferedImage) {
		BufferedImageLuminanceSource bils = new BufferedImageLuminanceSource(bufferedImage ); // Here I assumed the BufferdImage object name is "bufferedImage"
		HybridBinarizer hb = new HybridBinarizer(bils);//bils is BufferedImageLuminanceSource object
		return hb;
	}
	
	public static BufferedImage adjustContrastAndBrigtness(BufferedImage image) {
		RescaleOp rescaleOp = new RescaleOp(1.2f, 15, null);
		rescaleOp.filter(image, image);  // Source and destination are the same.
		//writeTo( image, "adjusted");
		return image;
	}

	public static BufferedImage rotate(BufferedImage src, double rotateAngle, double anchorW, double anchorH) {
		if ( Double.compare(rotateAngle,0F) == 0 ) {
			return src;
		}
		System.err.println("rotate:" + rotateAngle);
	    BufferedImage result = newBufferedImage(src.getWidth(), src.getHeight());
	    // Rotation information
	    AffineTransform tx = AffineTransform.getRotateInstance(rotateAngle,anchorW, anchorH);
	    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(src, result);
	    return result;
	}
    
	public static BufferedImage rotateBig(BufferedImage image, int angle) {
		if ( angle == 0) {
			return image;
		}
		if ( angle == 180) {
			return rotate(image, Math.toRadians(180), 0.5*image.getWidth(), 0.5*image.getHeight());
		}
		int w = image.getHeight(), h = image.getWidth();
	    BufferedImage result = newBufferedImage(w, h);
	    // Rotation information
	    AffineTransform tx = AffineTransform.getRotateInstance( Math.toRadians(angle), 0.5*h, 0.5*w);
	    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(image, result);
/*		
	    double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
	    int neww = (int)Math.floor(w*cos+h*sin), newh = (int)Math.floor(h*cos+w*sin);
	    BufferedImage result = newBufferedImage(neww, newh);
	    Graphics2D g = result.createGraphics();
	    g.translate((neww-w)/2, (newh-h)/2);
	    g.rotate(angle, w/2, h/2);
	    g.drawRenderedImage(image, null);
	    g.dispose();
	    */
	    return result;
	}	

	public static BufferedImage scaleByProportion(BufferedImage src, ResultPoint scale) {
		if ( noScale(scale)) {
			return src;
		}
		int newW = (int)(src.getWidth()*scale.getX());
		int newH = (int)(src.getHeight()*scale.getY());
		return toBufferedImage(src.getScaledInstance(newW, newH, Image.SCALE_FAST));		
	}

	private static boolean noScale(ResultPoint scale) {
		return scale.getX() == 1 && scale.getY() == 1;
	}
	
	public static BufferedImage scaleByProportion(BufferedImage src, float scale) {
		if ( Float.compare(scale, 1F) == 0 ) {
			return src;
		}
		int newW = (int) (src.getWidth()*scale);
		int newH = (int)(src.getHeight()*scale);
		
	    BufferedImage resizedImage = newBufferedImage(newW, newH);		
		/*
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        AffineTransformOp op = new AffineTransformOp(at, null);
        op.filter(src, resizedImage);
	    */
		resizedImage.getGraphics().drawImage(src.getScaledInstance(newW, newH, BufferedImage.SCALE_FAST), 0, 0, null);
	    return resizedImage;
	}
	// indexes of barcode points
	// 0----------X 
	// |
	// |  1     2
	// |      3
	// |  0
	// Y
	public static ResultPoint getScaleIncreaseProportion( ResultPoint[] coord, ResultPoint[] gauge ) {
		float left = coord[0].getY() - coord[1].getY();
		float top = coord[2].getX() - coord[1].getX();
			
		float gaugeLeft = gauge[0].getY() - gauge[1].getY();
		float gaugeTop = gauge[2].getX() - gauge[1].getX();
		
		return new ResultPoint(gaugeTop/top, gaugeLeft/left);
	}
	
	public static double getRotateAngle(ImgWith3QR img) {
		try {
			img.updateQR();
		} catch (NotFoundException|FormatException|ChecksumException e) {
			throw new RuntimeException(e);
		}
		return getRotateAngle(img.qr);
	}
	
	public static double getRotateAngle(ResultPoint[] barcodeCoordinates) {
		// [(210.0,437.5), (210.0,242.5), (405.0,240.0), (373.75,405.0)]
		// Работаем только с точками 1 и 0, этого достаточно
		ResultPoint pointBottom = barcodeCoordinates[0];
		ResultPoint pointTop = barcodeCoordinates[1];
		
		// если по X координаты не отличаются - значит поворота нет
		boolean leftSideRotated = Float.compare( pointBottom.getX(), pointTop.getX()) != 0;
		if ( !leftSideRotated /* && !topSideRotaited */	) {
			return 0;
		} 
		// вычисляем через синус угла у длинного катета, треугольник: 
		// смещение по X - короткий катет, смещение по Y - длинный катет
		float shortCathetus = pointBottom.getX() - pointTop.getX();
		float longCathetus = pointBottom.getY() - pointTop.getY();
		float hypotenuse = (float) Math.sqrt(Math.pow(shortCathetus, 2)+ Math.pow(longCathetus, 2));
		// sin(угол у длинного катета ) = противолежащий катет(короткий)/ гипотенузу
		double angleLong = Math.asin(shortCathetus/hypotenuse); 
		return angleLong;
	}


	/**
	 * Converts a given Image into a BufferedImage
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img){
	    if (img instanceof BufferedImage)  {
	        return (BufferedImage) img;
	    }
	    // Create a buffered image with transparency
	    BufferedImage bimage = newBufferedImage(img.getWidth(null), img.getHeight(null));
	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    // Return the buffered image
	    return bimage;
	}

	private static BufferedImage newBufferedImage(int w, int h) {
		return new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
	}	
	
	public static BufferedImage toBufferedImage(String fileName) {
		try {
			return toBufferedImage(ImageIO.read(new File(fileName)));
		} catch (IOException e) {
			throw new RuntimeException(fileName,e);
		}
	}

	public static BufferedImage cutOutPolygonShapedPart(BufferedImage src, Point leftTopCorner, int height, int width ) {
		// x and y place changed !!!
		return src.getSubimage(leftTopCorner.y, leftTopCorner.x, width, height);
	}

	public static byte[] toBytes(Image image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(toBufferedImage(image), "png", baos );
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ImgWithQR {
		public ImgWithQR(BufferedImage i, ResultPoint[] c) {
			img = i;
			qr = c;
		}
		public BufferedImage img;
		public ResultPoint[] qr;
		public String qrText;
		
		public void updateQR() throws NotFoundException, FormatException, ChecksumException {
			qr = detectBarcode(img);
		}
	}
	
	public static class ImgWith3QR extends ImgWithQR {

		public ImgWith3QR(BufferedImage i, ResultPoint[] c) {
			super(i, c);
		}

		public void updateQR() throws NotFoundException, FormatException, ChecksumException {
			Result qrCode = getBarcode3(img);
			qrText =  qrCode.getText();
			qr = qrCode .getResultPoints();
		}

		public int getBigAngle() {
			BufferedImage[] images = splitTo4Parts(img);
			
			boolean leftTop = search( images[0]);
			boolean rightTop = search( images[1]);
			boolean leftBottom = search( images[2]);
			boolean rightBottom = search( images[3]);
			
			if ( leftTop && leftBottom && rightTop && !rightBottom) {
				return 0;
			}
			if ( leftTop && !leftBottom && rightTop && rightBottom) {
				return 90;
			}
			if ( !leftTop && leftBottom && rightTop && rightBottom) {
				return 180;
			}
			if ( leftTop && leftBottom && !rightTop && rightBottom) {
				return 270;
			}
			throw new RuntimeException("Not all barcodes detected");
		}

		public BufferedImage rotateBig() {
			int bigAngle = getBigAngle();
			if ( bigAngle  != 0) {
				img = ImageUtil.rotateBig(img, -bigAngle);
			}
			return img;
		}
		
	}
	
	public static BufferedImage adjustByBarcode(ImgWith3QR img, ResultPoint[] gauge) throws NotFoundException, FormatException, ChecksumException {
		
		float rotateAngleBig = img.getBigAngle();
		if ( Float.compare(rotateAngleBig, 0F ) != 0 ) {
			img.img = rotate(img.img, -rotateAngleBig, 0,0);
			img.updateQR();
		}
		if ( img.qr == null ) {
			img.updateQR();
		}
		double rotateAngleRadians = getRotateAngle(img.qr);
		if ( Double.compare(rotateAngleRadians, 0d ) != 0 ) {
			img.img = rotate(img.img, rotateAngleRadians, img.qr[1].getX(), img.qr[1].getY() );
			img.updateQR();
			img = adjustRotation(img);
		}
		ResultPoint scale = getScaleIncreaseProportion(img.qr, gauge);
		if ( !noScale(scale)) {
			img.img = scaleByProportion(img.img, scale);
			img.updateQR();			
		}

		img.img = adjustBarcodePosition(img, gauge);
		
		img.img = cutFromRightAndBottom(img);
		
		img.updateQR();
		return img.img;
	}
	
	public static BufferedImage cutFromRightAndBottom(ImgWithQR img) {
		int h = (int) (img.qr[0].getY() + 100);
		int w = (int) (img.qr[2].getX() + 100);
		return setSize( img.img, w, h );
	}

	public static ImgWith3QR adjustRotation(ImgWith3QR imgWithQR) throws NotFoundException, FormatException, ChecksumException {
		double xDiff = (int) (imgWithQR.qr[0].getX() - imgWithQR.qr[1].getX());
		int iterations = 7;
		while ( Math.abs(xDiff) > 0.3 && iterations-- > 0) {
			// writeDebug(imgWithQR.img);
			// 0.0005F * (xDiff > 0 ? 1 : -1)
			System.err.println( "adjusting..." + xDiff);
			imgWithQR.img = rotate(imgWithQR.img, 0.00014F * xDiff, imgWithQR.qr[1].getX(), imgWithQR.qr[1].getY());
			imgWithQR.updateQR();
			xDiff = imgWithQR.qr[0].getX() - imgWithQR.qr[1].getX();				
		}
		return imgWithQR;
	}


	private static boolean search( BufferedImage img) {
		try {
			detectBarcode(img);
			return true;
		} catch (NotFoundException | FormatException | ChecksumException e) {
			//writeDebug(img);
		}
		return false;
	}

	private static BufferedImage adjustBarcodePosition(ImgWithQR image, ResultPoint[] gauge) {
		int shiftTop = (int) (gauge[1].getY() - image.qr[1].getY());
		int shiftLeft = (int) (gauge[1].getX() - image.qr[1].getX());
		return shiftImage(image.img, shiftTop, shiftLeft);
	}

	public static BufferedImage shiftImage(BufferedImage image, int shiftFromTop, int shiftFromLeft) {
		if ( shiftFromLeft == 0 && shiftFromTop == 0) {
			return image;
		}
		// Create a buffered image with transparency
	    BufferedImage result = newBufferedImage(image.getWidth(), image.getHeight());
	    // Draw the image on to the buffered image
	    Graphics2D bGr = result.createGraphics();
	    if ( shiftFromLeft < 0 && shiftFromTop < 0) {
	    	
	    	int w = image.getWidth() - Math.abs(shiftFromLeft);
			int h = image.getHeight() - Math.abs(shiftFromTop);
			bGr.drawImage( image.getSubimage(Math.abs(shiftFromLeft), Math.abs(shiftFromTop), w, h), 0, 0, null);
	    } else if ( shiftFromLeft < 0) {
	    	int w = image.getWidth() - Math.abs(shiftFromLeft);
			bGr.drawImage( image.getSubimage(Math.abs(shiftFromLeft), 0, w, image.getHeight() - shiftFromTop), 0, shiftFromTop, null);

		} else if ( shiftFromTop < 0) {
			int h = image.getHeight() - Math.abs(shiftFromTop);
			bGr.drawImage( image.getSubimage(0, Math.abs(shiftFromTop), image.getWidth() - shiftFromLeft, h), shiftFromLeft, 0, null);

	    } else { // both > 0
	    	bGr.drawImage( image, shiftFromLeft, shiftFromTop, null);	    	
	    }
	    bGr.dispose();
	    // Return the buffered image
	    //writeDebug(result);
	    return result;			
	}

	public static BufferedImage setSize(BufferedImage image, int w, int h) {
		if ( image.getWidth() == w && image.getHeight() == h) {
			return image;
		}
		if ( image.getWidth() >= w && image.getHeight() >= h) {
			return image.getSubimage(0, 0, w, h);
		}
		BufferedImage resizedImage = newBufferedImage(w, h);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(image, 0, 0, w, h, null);
		g.dispose();			
		return resizedImage;
	}
	
	
	/**
	 * Normalizing vertical and horizontal barcode size and it rotation position 
	 * @param barCode
	 * @return
	 */
	public static ResultPoint[] adjustBarcodeSize( ResultPoint[] pt) {
		// координаты баркода: (335.0,685.0) (335.0,385.0) (640.0,385.0)
		ResultPoint leftBottom = pt[0];
		ResultPoint leftTop = pt[1];
		ResultPoint rightTop = pt[2];
		//
		int corner = (int) Math.min(leftTop.getX(), leftTop.getY());
		// int side = (int) Math.max(leftBottom.getY()- leftTop.getY(), rightTop.getX() - leftTop.getX());
		ResultPoint newLeftTop = new ResultPoint(corner, corner);
		ResultPoint newLeftBottom = new ResultPoint(corner, leftBottom.getY());
		ResultPoint newRightTop = new ResultPoint(rightTop.getX(), corner);
		
		return new ResultPoint[] { newLeftBottom, newLeftTop, newRightTop };
	}

	static int diffW = 20; // 20
	static int diffH = 22; // 22
	
	static int shiftLeftPerc = 10; // 10
	static int shiftTopPerc = 5; // 5
	
	public static List<Double> imageDiffWithShift(BufferedImage testBI, BufferedImage trainBI) {
		trainBI = adjustScaleTo(trainBI, diffW, diffH);
		testBI = adjustScaleTo(testBI, diffW, diffH);
		
		List<Double> result = new LinkedList<>();
		try {		
			result.add(imageDiff(testBI, trainBI));
		} catch (RuntimeException e) {
			System.err.println( e.getMessage());
		}
		
		int stepLeft = (int) (new Double(diffW)/100*shiftLeftPerc);
		int stepTop = (int) (new Double(diffH)/100*shiftTopPerc);
		for (int left = -stepLeft; left <= stepLeft; left++) {
			for (int top = -stepTop; top <= stepTop; top++) {
				BufferedImage testTemp = shiftImage(testBI, top, left);
				try {
					result.add(imageDiff(testTemp, trainBI));
				} catch (RuntimeException e) {
					System.err.println( e.getMessage());
				}
			}
		}
		
		return result;
	}
	
	public static Double imageDiff(BufferedImage testBI, BufferedImage trainBI) {

		trainBI = adjustScaleTo(trainBI, diffW, diffH);
		testBI = adjustScaleTo(testBI, diffW, diffH);
		
		BitMatrix bitsTest = toBitMatrixBlack(testBI);
		BitMatrix bitsTrain = toBitMatrixBlack(trainBI);

		Double diffPercents = diffPercents( bitsTest, bitsTrain);
		// writeDebug(trainBI, "train");
		// writeDebug(testBI, "test");
		return diffPercents;
	}
	
	private static Double diffPercents(BitMatrix bitsLeft, BitMatrix bitsRight) {
		int height = bitsLeft.getHeight();
		int width = bitsLeft.getWidth();
		int diffBits = 0;
		for (int x = 0; x < height; x++) {
			for (int y = 0; y < width; y++) {
				boolean diff = bitsLeft.get(x, y) ^ bitsRight.get(x, y);
				if ( diff ) {
					++diffBits;
				}
			}
		}
		int totalBits = height*width;
		return new Double(diffBits)/(new Double(totalBits)/100);
	}

	public static BufferedImage adjustScaleTo(BufferedImage src, int w, int h) {
		BufferedImage bi = scaleByProportion(src, new ResultPoint(new Float(w)/src.getWidth(), new Float(h)/src.getHeight()));
		if ( bi.getWidth() != w || bi.getHeight() != h ) {
			System.err.println( "Setting size! It may turn to badly diff detection [ w:" + w + "(" + bi.getWidth() + ") h:" + h + "(" + bi.getHeight() +")]");
			return ImageUtil.setSize(bi, w, h);
		}
		return bi;
	}

	// Image Parsing
	/*
	public static parse() { 
	    int[] data = ColorUtils.getImageData(img);
	    bounds = new Rectangle(img.getWidth(), img.getHeight());
	    int wt = (int) bounds.getWidth();
	    int ht = (int) bounds.getHeight();
	    bitmasks = new long[ht];
	    for(int y = 0; y < ht; y++) {
	        long bitmask = 0;
	        for(int x = 0; x < wt; x++) {
	            if(data[x + y * wt] != excludedColor)
	                bitmask = (bitmask << 1) + 1;
	            else
	                bitmask = bitmask << 1;
	        }
	        bitmasks[y] = bitmask;
	    }
	}
    // File Printing

    PrintWriter pw =  null;
    try {
        pw = new PrintWriter(new File("/home/brian/Desktop/imageModel.txt"));
    } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
    }
    for(int y = 0; y < bounds.getHeight(); y++) {
        long bitmask = bitmasks[y];
        for(int x = 0; x < bounds.getWidth(); x++) {
            pw.print(String.valueOf((bitmask >> (x + y * (int) bounds.getWidth())) & 1));
        }
        pw.println();
        pw.println();
    }
    pw.close();	
*/
	static final int BLACK = 0;
	static final int WHITE = 255;
	static final int BYTE_WHITE = 18;
	
	public static BufferedImage toBufferedImageGrayScale(int width, int height, int[] pixels) {
		BufferedImage image = new BufferedImage(width,height, BufferedImage.TYPE_BYTE_GRAY );
		image.setRGB(0, 0, width, height, pixels, 0, width );
		return fromGrayScale(image);
	}

	public static BufferedImage toBufferedImage(int width, int height, int[] pixels) {
		BufferedImage image = newBufferedImage(width,height);
		image.setRGB(0, 0, width, height, pixels, 0, width );
		return image;
	}

	public static BufferedImage fromGrayScale(BufferedImage image) {
	    BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
	    result.getGraphics().drawImage(image, 0, 0, null);
	    WritableRaster raster = result.getRaster();
	    int[] pixels = new int[image.getWidth()];
	    for (int y = 0; y < image.getHeight(); y++) {
	        raster.getPixels(0, y, image.getWidth(), 1, pixels);
	        for (int i = 0; i < pixels.length; i++) {

	            pixels[i] = grayByteToRGB(pixels[i]);
	        }
	        raster.setPixels(0, y, image.getWidth(), 1, pixels);
	    }
	    return result;
	}

	private static int grayByteToRGB(int pixel) {
		return pixel*(WHITE/BYTE_WHITE);
	}
    
    public static int[] getPixels(int[]src, int x2, int x1, int y2, int y1, int widthOrginal) {
        
        int heightOrginal = src.length / widthOrginal;
        if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 
                || x1 > x2 || y1 > y2 
                || x1 >= widthOrginal || x2 >= widthOrginal || y2 > heightOrginal) {
            throw new IllegalArgumentException("Incorrect coordinates [" + x1 + ":" + x2 + ":"+ y1 + ":"+ y2 + ":"+ widthOrginal + ":" + heightOrginal + "]");
        }
        y2 = Math.min(y2, heightOrginal-1);
        int width = x2 - x1 + 1;
        int height = y2 - y1 + 1; 
        int[] pixels = new int[height*width];
        for (int y = y1, idx = 0; y <= y2; y++) {
               for (int x = x1; x <= x2; x++, idx++) {
                      pixels[idx] = src[x + y * widthOrginal];
               }
        }
        return pixels;
  }

}