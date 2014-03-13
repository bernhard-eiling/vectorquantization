package eiling.vectorquant;

import java.io.Serializable;
import java.util.ArrayList;

public class Codebook implements Serializable {

	ArrayList<int[]> vectors = new ArrayList<int[]>();
	ArrayList<Integer> means = new ArrayList<Integer>();
	int blockSide;
	int blockSize;
	int numVectors;

	public Codebook(int blockSide, int numVectors) {
		this.blockSide = blockSide;
		this.blockSize = blockSide * blockSide;
		this.numVectors = numVectors;
	}

	public void generateRandomCodebook() {
		for (int i = 0; i < numVectors; i++) {
			int[] vector = new int[blockSize * blockSize];
			int val = (int) (Math.random() * 255.0d);
			// fill random sample vector
			for (int v = 0; v < vector.length; v++) {
				vector[v] = val;
			}
			vectors.add(i, vector);
			means.add(i, getMeanOfVector(vector));
		}
	}

	private double calcMSE(int[] pic, int[] codebook) {

		int mse = 0;

		for (int i = 0; i < codebook.length; i++) {
			int error = pic[i] - codebook[i];
			mse += error * error;

		}
		mse /= codebook.length;
		return mse;
	}

	// codedImage holds indexes of best codebook entry
	public int[][] codeImage(ImageView srcImage, int[][] codebook) {
		int numBlocksX = (int)((double)srcImage.getImgWidth() / blockSide);
		int numBlocksY = (int)((double)srcImage.getImgHeight() / blockSide);
		int w = srcImage.getImgWidth();
		int[][] codedImage = new int[numBlocksY][numBlocksX];
		int[] pixels = srcImage.getPixels();

		for (int yBlock = 0; yBlock < numBlocksY; yBlock++) {
			for (int xBlock = 0; xBlock < numBlocksX; xBlock++) {
				int[] srcImageBlockVals = new int[blockSize];
				for (int y = 0; y < blockSide; y++) {
					for (int x = 0; x < blockSide; x++) {
						int pos = this.getPos(w, xBlock, yBlock, x, y);
						int val = (pixels[pos] >> 16) & 255;
						srcImageBlockVals[y * blockSide + x] = val;
					}
				}
				double minMse = Double.MAX_VALUE;
				int minIndex = -1;
				// get best codebook entry
				for (int i = 0; i < codebook.length; i++) {
					double mse = this.calcMSE(srcImageBlockVals, codebook[i]);
					if (mse < minMse) {
						minMse = mse;
						minIndex = i;
					}
				}
				codedImage[yBlock][xBlock] = minIndex;
			}
		}
		return codedImage;
	}
	
	public int[] decodeImage(int w, int h, int[][] codedImage, int[][] codebook) {
		int[] returnPixels = new int[w * h];
		for (int yBlock = 0; yBlock < codedImage.length; yBlock++) {
			for (int xBlock = 0; xBlock < codedImage[yBlock].length; xBlock++) {
				int[] vector = codebook[codedImage[yBlock][xBlock]];
				for (int y = 0; y < blockSide; y++) {
					for (int x = 0; x < blockSide; x++) {
						int pos = this.getPos(w, xBlock, yBlock, x, y);
						int posBlock = y * blockSide + x;
						int val = 0xFF000000 + ((vector[posBlock] & 0xff) << 16) + ((vector[posBlock] & 0xff) << 8) + (vector[posBlock] & 0xff);
						returnPixels[pos] = val;
					}
				}
			}
		}
		return returnPixels;
	}

	public int[][] generateCodebookFromImage2dVector(double[][] vectorsFromImage) {

		//
		// anfang von LBG ermitteln
		int[] beginnVal = new int[blockSize];

		int len = vectorsFromImage.length;
		for (int i = 0; i < vectorsFromImage.length; i++) {
			for (int valI = 0; valI < vectorsFromImage[i].length; valI++) {
				double val = vectorsFromImage[i][valI];
				beginnVal[valI] += val;
			}
		}

		// numCols = vektor l�nge von block
		// es werden cluster entsprechend der anzahl der codebuch eintr�ge
		// initalisiert
		Cluster[] cluster = new Cluster[numVectors];
		// initialize cluster 0 with the mean color
		// die vals des ersten clusters entsprechen dem durchschnitt
		cluster[0] = new Cluster(blockSize);
		for (int i = 0; i < blockSize; i++) {
			cluster[0].val[i] = beginnVal[i] / len;
		}

		// iteration um alle ( = anzahl der samples im codebuch) cluster zu
		// berechnen

		for (int iVec = 0; iVec < numVectors; iVec++) {
		    System.out.println("cluster num: " + iVec);
			double lastErr = Double.MAX_VALUE;
			int nMax = -1;
			boolean doIterate;
			int it = 0;
			do {
				double totalErr = 0;
				// bei jeder iteration werden val summen �berschrieben
				//
				for (int k = 0; k <= iVec; k++) {
					Cluster cl = cluster[k];
					for (int i = 0; i < cl.valSum.length; i++) {
						cl.valSum[i] = 0;
					}
					cl.count = 0;
					cl.err = 0;
				}

				// alle vektoren/ blöcke werden iteriert
				// für jeden pixel wird der nächstgelegene cluster ermittelt
				// auf diesen werden die entsprechenden pixelwerte aufsummiert

				for (int i = 0; i < vectorsFromImage.length; i++) {

						double[] vectorFromImage = vectorsFromImage[i];

						// die distanz von jedem cluster zu einem pixel
						// value wird errechnet
						// daraus wird der quadratische fehler errechnet
						// minK = clusterindex mit minimalen fehler
						double minErr = Double.MAX_VALUE;
						int minK = -1;
						for (int k = 0; k <= iVec; k++) {
							Cluster cl = cluster[k];
							double[] deltaVal = new double[blockSize];
							for (int iVal = 0; iVal < blockSize; iVal++) {
								deltaVal[iVal] = vectorFromImage[iVal]
										- cl.val[iVal];
							}
							double err = 0;
							for (int iErr = 0; iErr < blockSize; iErr++) {
								err += deltaVal[iErr] * deltaVal[iErr];
							}
							if (err < minErr) {
								minErr = err;
								minK = k; // index of cluster with min err
							}
						}
						// count++ = ein pixel mehr
						// n = sums of values
						Cluster cl = cluster[minK];
						for (int j = 0; j < blockSize; j++) {
							cl.valSum[j] += vectorFromImage[j];
						}
						cl.count++;
						cl.err += minErr;

				}

					// search the cluster with max error
					// nMax = index of cluster of max error
				double maxErr = 0;
				for (int n = 0; n <= iVec; n++) {
					Cluster cl = cluster[n];
					totalErr += cl.err;
					if (cl.err >= maxErr) {
						maxErr = cl.err;
						nMax = n;
					}
				}


				// abbruchbedingung -> wenn sich der fehler kaum noch ändert
				doIterate = iVec > 0 && it++ < 20 && lastErr > totalErr
						&& lastErr / totalErr > 1.001;

				// update the clusters with the new values
				if (doIterate) {
					for (int n = 0; n <= iVec; n++) {
						Cluster cl = cluster[n];
						if (cl.count > 0) {
							for (int iVal = 0; iVal < blockSize; iVal++) {
								cl.val[iVal] = cl.valSum[iVal] / cl.count;
							}
						}
					}
					// System.out.println(totalErr);
					lastErr = totalErr;
				}

			} while (doIterate == true);

			// add new a cluster (split the worst)
			if (iVec < numVectors - 1) {
				cluster[iVec + 1] = new Cluster(blockSize);
				for (int iVal = 0; iVal < blockSize; iVal++) {
					cluster[iVec + 1].val[iVal] = cluster[nMax].val[iVal] - 0.01;
					cluster[nMax].val[iVal] += 0.01;
				}
			}
		}

		// finale anzahl aller cluster
		int finalSize = 0;
		for (int i = 0; i < cluster.length; i++) {
			if (cluster[i].count > 0)
				finalSize++;
		}
		int[][] vectorList = new int[finalSize][blockSize];

		// erstellen der codebucheintr�ge
		for (int j = 0; j < vectorList.length; j++) {
			double maxCount = 0;
			int maxCountIndex = -1;
			for (int i = 0; i < cluster.length; i++) {
				Cluster cl = cluster[i];
				if (cl.count > maxCount) {
					maxCount = cl.count;
					maxCountIndex = i;
				}
			}
			for (int iVec = 0; iVec < blockSize; iVec++) {
				vectorList[j][iVec] = (int) cluster[maxCountIndex].val[iVec];
			}
//			vectorList[j][blockSize] = (int) (cluster[maxCountIndex].count * 100. / len + 0.5);
			cluster[maxCountIndex].count = 0;
		}

		return vectorList;
	}

    public int[][] generateCodebookFromImage3dVector(int[][][] vectorsFromImage) {

        //
        // anfang von LBG ermitteln
        int[] beginnVal = new int[blockSize];

        int len = vectorsFromImage.length * vectorsFromImage[0].length;
        for (int y = 0; y < vectorsFromImage.length; y++) {
            for (int x = 0; x < vectorsFromImage[y].length; x++) {
                for (int valI = 0; valI < vectorsFromImage[y][x].length; valI++) {
                    int val = vectorsFromImage[y][x][valI];
                    beginnVal[valI] += val;
                }
            }
        }

        // numCols = vektor l�nge von block
        // es werden cluster entsprechend der anzahl der codebuch eintr�ge
        // initalisiert
        Cluster[] cluster = new Cluster[numVectors];
        // initialize cluster 0 with the mean color
        // die vals des ersten clusters entsprechen dem durchschnitt
        cluster[0] = new Cluster(blockSize);
        for (int i = 0; i < blockSize; i++) {
            cluster[0].val[i] = beginnVal[i] / len;
        }

        // iteration um alle ( = anzahl der samples im codebuch) cluster zu
        // berechnen

        for (int iVec = 0; iVec < numVectors; iVec++) {
            // System.out.println("n = " + (nc+1));
            double lastErr = Double.MAX_VALUE;
            int nMax = -1;
            boolean doIterate;
            int it = 0;
            do {
                double totalErr = 0;
                // bei jeder iteration werden val summen �berschrieben
                //
                for (int k = 0; k <= iVec; k++) {
                    Cluster cl = cluster[k];
                    for (int i = 0; i < cl.valSum.length; i++) {
                        cl.valSum[i] = 0;
                    }
                    cl.count = 0;
                    cl.err = 0;
                }

                // alle vektoren/ blöcke werden iteriert
                // für jeden pixel wird der nächstgelegene cluster ermittelt
                // auf diesen werden die entsprechenden pixelwerte aufsummiert

                for (int y = 0; y < vectorsFromImage.length; y++) {
                    for (int x = 0; x < vectorsFromImage[y].length; x++) {

                        int[] vectorFromImage = vectorsFromImage[y][x];

                        // die distanz von jedem cluster zu einem pixel
                        // value wird errechnet
                        // daraus wird der quadratische fehler errechnet
                        // minK = clusterindex mit minimalen fehler
                        double minErr = Double.MAX_VALUE;
                        int minK = -1;
                        for (int k = 0; k <= iVec; k++) {
                            Cluster cl = cluster[k];
                            double[] deltaVal = new double[blockSize];
                            for (int iVal = 0; iVal < blockSize; iVal++) {
                                deltaVal[iVal] = vectorFromImage[iVal]
                                        - cl.val[iVal];
                            }
                            double err = 0;
                            for (int iErr = 0; iErr < blockSize; iErr++) {
                                err += deltaVal[iErr] * deltaVal[iErr];
                            }
                            if (err < minErr) {
                                minErr = err;
                                minK = k; // index of cluster with min err
                            }
                        }
                        // count++ = ein pixel mehr
                        // n = sums of values
                        Cluster cl = cluster[minK];
                        for (int j = 0; j < blockSize; j++) {
                            cl.valSum[j] += vectorFromImage[j];
                        }
                        cl.count++;
                        cl.err += minErr;

                    }
                }

                // search the cluster with max error
                // nMax = index of cluster of max error
                double maxErr = 0;
                for (int n = 0; n <= iVec; n++) {
                    Cluster cl = cluster[n];
                    totalErr += cl.err;
                    if (cl.err >= maxErr) {
                        maxErr = cl.err;
                        nMax = n;
                    }
                }

                // abbruchbedingung -> wenn sich der fehler kaum noch ändert
                doIterate = iVec > 0 && it++ < 20 && lastErr > totalErr
                        && lastErr / totalErr > 1.001;

                // update the clusters with the new values
                if (doIterate) {
                    for (int n = 0; n <= iVec; n++) {
                        Cluster cl = cluster[n];
                        if (cl.count > 0) {
                            for (int iVal = 0; iVal < blockSize; iVal++) {
                                cl.val[iVal] = cl.valSum[iVal] / cl.count;
                            }
                        }
                    }
                    // System.out.println(totalErr);
                    lastErr = totalErr;
                }

            } while (doIterate == true);

            // add new a cluster (split the worst)
            if (iVec < numVectors - 1) {
                cluster[iVec + 1] = new Cluster(blockSize);
                for (int iVal = 0; iVal < blockSize; iVal++) {
                    cluster[iVec + 1].val[iVal] = cluster[nMax].val[iVal] - 0.01;
                    cluster[nMax].val[iVal] += 0.01;
                }
            }
        }

        // finale anzahl aller cluster
        int finalSize = 0;
        for (int i = 0; i < cluster.length; i++) {
            if (cluster[i].count > 0)
                finalSize++;
        }
        int[][] vectorList = new int[finalSize][blockSize];

        // erstellen der codebucheintr�ge
        for (int j = 0; j < vectorList.length; j++) {
            double maxCount = 0;
            int maxCountIndex = -1;
            for (int i = 0; i < cluster.length; i++) {
                Cluster cl = cluster[i];
                if (cl.count > maxCount) {
                    maxCount = cl.count;
                    maxCountIndex = i;
                }
            }
            for (int iVec = 0; iVec < blockSize; iVec++) {
                vectorList[j][iVec] = (int) cluster[maxCountIndex].val[iVec];
            }
//			vectorList[j][blockSize] = (int) (cluster[maxCountIndex].count * 100. / len + 0.5);
            cluster[maxCountIndex].count = 0;
        }

        return vectorList;
    }

	private int getMeanOfVector(int[] vector) {
		int mean = 0;
		for (int v = 0; v < vector.length; v++) {
			mean += vector[v];
		}
		mean = mean / vector.length;
		return mean;
	}

	public ArrayList<int[]> getVectors() {
		return this.vectors;
	}

	public ArrayList<Integer> getMeans() {
		return this.means;
	}

	public int getBlockSize() {
		return blockSize;
	}

	private int getPos(int w, int xBlock, int yBlock, int x, int y) {
		// y * w + x
		int pos = (yBlock * blockSide + y) * w + xBlock * blockSide + x;
		return pos;
	}
	
	public void update(int blockSide, int numVectors) {
		this.blockSide = blockSide;
		this.blockSize = blockSide * blockSide;
		this.numVectors = numVectors;
	}
}

class Cluster {
	double count;
	double[] val;
	double[] valSum;
	double err;

	Cluster(int valNum) {
		val = new double[valNum];
		valSum = new double[valNum];
	}
}