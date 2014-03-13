package eiling.vectorquant;

import java.util.ArrayList;

public class ImgAsVectors {
	
	private ImageView imageView;
	private int blockSize;
	private int vectorSize;
	private int numBlocksY;
	private int numBlocksX;
    private int numBlocks;
//	private ArrayList<ArrayList<int[]>> imageVectors = new ArrayList<ArrayList<int[]>>(); 
//	private ArrayList<ArrayList<Integer>> vectorMeans = new ArrayList<ArrayList<Integer>>();
	private int[][][] imageVectors;
	private int[][] vectorMeans;
    private double[][] imageVectorsRBM;

	public ImgAsVectors(ImageView imageView, int blockSide) {
		
		this.imageView = imageView;
		this.blockSize = blockSide;
		this.vectorSize = blockSide * blockSide;
		numBlocksX = imageView.getImgWidth() / blockSide;
		numBlocksY = imageView.getImgHeight() / blockSide;
        numBlocks = numBlocksX * numBlocksY;
		imageVectors = new int[numBlocksY][numBlocksX][vectorSize];
		vectorMeans = new int[numBlocksY][numBlocksX];
        imageVectorsRBM = new double[numBlocks + 1][vectorSize];
		
		// init imageVectors / vectorMeans
//		for (int y = 0; y <= numBlocksY; y++) {			
//			imageVectors.add(y, new ArrayList<int[]>());
//			vectorMeans.add(y, new ArrayList<Integer>());
//			for (int x = 0; x <= numBlocksX; x++) {
//				imageVectors.get(y).add(x, new int[vectorSize]);
//				vectorMeans.get(y).add(x, -1);
//			}
//		}
	}

    public int[][][] getVectors() {
		// loop blocks
		int pixels[] = imageView.getPixels();
		for (int yBlock = 0; yBlock < numBlocksY; yBlock++) {
			for (int xBlock = 0; xBlock < numBlocksX; xBlock++) {
				int vectorIndex = 0;
				// iterate block
				for (int y = 0; y < blockSize; y++) {
					for (int x = 0; x < blockSize; x++) {
						// index in imageView
						int pos = this.getPos(imageView.getImgWidth(), xBlock, yBlock, x, y);
						// write pixel in vector
						// WORKS ONLY WITH GREY IMG !!!
//						System.out.println("(pixels[pos] & 0xff): " + (pixels[pos] & 0xff));
						imageVectors[yBlock][xBlock][vectorIndex] = (pixels[pos] & 0xff);
						vectorIndex++;
					}
				}
			}
		}
        return imageVectors;
	}

    public double[][] getVectors2d() {
        // loop blocks
        int pixels[] = imageView.getPixels();
        int blockIndex = 0;
        for (int yBlock = 0; yBlock < numBlocksY; yBlock++) {
            for (int xBlock = 0; xBlock < numBlocksX; xBlock++) {
                int vectorIndex = 0;
                // iterate block
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        // index in imageView
                        int pos = this.getPos(imageView.getImgWidth(), xBlock, yBlock, x, y);
                        // write pixel in vector
                        // WORKS ONLY WITH GREY IMG !!!
//						System.out.println("(pixels[pos] & 0xff): " + (pixels[pos] & 0xff));
                        imageVectorsRBM[blockIndex][vectorIndex] = (pixels[pos] & 0xff);
                        /*
                        System.out.println("pos: " + pos);
                        System.out.println("blockIndex: " + blockIndex);
                        System.out.println("vectorIndex: " + vectorIndex + "\n");
                        */
                        vectorIndex++;
                    }
                }
                blockIndex++;
            }
        }
        return imageVectorsRBM;
    }

	public void generateMeanOfVectors() {
//		System.out.println("imageVectors.length: " + imageVectors.length);
//		System.out.println("numBlocksY: " + numBlocksY);
		for (int yBlock = 0; yBlock < imageVectors.length; yBlock++) {
			for (int xBlock = 0; xBlock < imageVectors[yBlock].length; xBlock++) {
				int valSum = 0;
				for (int i = 0; i < imageVectors[yBlock][xBlock].length; i++) {
//					System.out.println("imageVectors[y][x][i]: " + imageVectors[y][x][i]);
					valSum += imageVectors[yBlock][xBlock][i];
				}
				valSum = valSum/vectorSize;
				vectorMeans[yBlock][xBlock] = valSum;
			}
		}
	}
	
	/*
	 * DEPRECATED
	 */
	public ImageView synthesizeImage(Codebook codebook) {
		ImageView synthImage = new ImageView(imageView.getImgWidth(), imageView.getImgHeight());
		int[] synthPixels = synthImage.getPixels();
		ArrayList<Integer> means = codebook.getMeans();
		
        for (int yBlock = 0; yBlock < vectorMeans.length; yBlock++) {
        	for (int xBlock = 0; xBlock < vectorMeans[yBlock].length; xBlock++) {
        		
        		// get closest codebook sample for vector
        		int meanOfVector = vectorMeans[yBlock][xBlock];
        		int distance = 255;
        		int closest = -1;
        		for (int m : means) {
        			int delta = Math.abs(m - meanOfVector);
        			if (delta < distance) {
        				distance = delta;
        				closest = m;
        			}
        		}
        		
        		// write sample into block
        		for (int y = 0; y < this.blockSize; y++) {
        			for (int x = 0; x < this.blockSize; x++) {
        				int pos = this.getPos(synthImage.getImgWidth(), xBlock, yBlock, x, y);
        				synthPixels[pos] = 0xFF000000 + ((closest & 0xff) << 16) + ((closest & 0xff) << 8)
        						+ (closest & 0xff);
        			}
        		} 		
        	}
        }
        synthImage.applyChanges();
        return synthImage;
	}
	////////////////////////7
	
	private int getPos(int w, int xBlock, int yBlock, int x, int y) {
		// y * w + x
		int pos = (yBlock * blockSize + y) * w + xBlock * blockSize + x;
		return pos;
	}
}
