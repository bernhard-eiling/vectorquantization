package eiling.vectorquant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Vectorquantization extends JPanel {
	
	////////////////////////////////////////////////////
	private Integer vectorSide = 4;
    private Integer vectorSize = vectorSide * vectorSide;
    private int codebookSize = 128;
    private int[][] codebookVectors;
	Codebook book;
	
	//////////////////////////////////////////////////////
	
	private static final long serialVersionUID = 1L;
	private static final int borderWidth = 5;
	private static final int maxWidth = 520;
	private static final int maxHeight = maxWidth;
	
	private static JFrame frame;
	
	private ImageView srcView;			// source image view
	private ImageView recView;			// reconstruction image view
    private ImageView negView;

	private ImgPanel srcPanel;
	private ImgPanel recPanel;
    private ImgPanel negPanel;

    JPanel samplesPanel = new JPanel(new GridBagLayout());
    JPanel controls = new JPanel(new GridBagLayout());
    JPanel dataPanel = new JPanel(new GridLayout(3, 1));


    private int selectedBlockIndex = 1;
    private String imgName = "LenaBW.png";

    ArrayList<int[][]> listOfCodeBookVectors = new ArrayList<int[][]>();
    ArrayList<int[]> listOfDecodedImages = new ArrayList<int[]>();
    ArrayList<Double> listOfMSEs = new ArrayList<Double>();
    ArrayList<Double> listOfBpp = new ArrayList<Double>();
    ArrayList<Double> listOfPSNR = new ArrayList<Double>();

	public Vectorquantization() {
		
		super(new BorderLayout(borderWidth, borderWidth));

        setBorder(BorderFactory.createEmptyBorder(borderWidth,borderWidth,borderWidth,borderWidth));
        
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0,borderWidth,0,0);
 
        // load the default image
        File input = new File("data/" + imgName);
        
        if(!input.canRead()) input = openFile(); // file not found, choose another image
        
        srcView = new ImageView(input);
        srcView.setMaxSize(new Dimension(maxWidth, maxHeight));
       
		// create empty destination images
		recView = new ImageView(srcView.getImgWidth(), srcView.getImgHeight());
		recView.setMaxSize(new Dimension(maxWidth, maxHeight));

        negView = new ImageView(srcView.getImgWidth(), srcView.getImgHeight());
        negView.setMaxSize(new Dimension(maxWidth, maxHeight));

        ////////////////////////
		// BUTTONS
        JButton loadPic = new JButton("Bild öffnen");
        loadPic.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		loadFile(openFile());
        		// process();
        	}        	
        });
        controls.add(loadPic, c);
   /*
        JButton loadCoodebook = new JButton("Codebuch öffnen");
        loadCoodebook.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
                loadCodebook(openFile());
        		// process();
        	}        	
        });
        controls.add(loadCoodebook, c);

        JButton safeCodebook = new JButton("Codebuch speichern");
        loadCoodebook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                safeCodebook();
            }
        });
        controls.add(safeCodebook, c);
        */

        JButton loadFolderButton = new JButton("Ordner öffnen, trainieren und RUN");
        loadFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFolder();
            }
        });
        controls.add(loadFolderButton, c);

        JButton run = new JButton("Originalbild traineren und RUN");
        run.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trainFromSrc();
            }
        });
        controls.add(run, c);

        ////////////////////////////////////////
        // images panel
        JPanel images = new JPanel(new GridLayout(1,2));
        srcPanel = new ImgPanel(srcView, "Originalbild", " ");
        recPanel = new ImgPanel(recView, "Rekonstruiertes Bild", " ");
        negPanel = new ImgPanel(negView, "Negativbild", " ");
        images.add(srcPanel);
        images.add(recPanel);
        images.add(negPanel);

        ////////////////////////////////////
        add(controls, BorderLayout.NORTH);
        add(images, BorderLayout.CENTER);
        add(dataPanel, BorderLayout.WEST);
        add(samplesPanel, BorderLayout.SOUTH);

        /////////////////////////////
    	if (book == null) {
    		book = new Codebook(vectorSide, codebookSize);
    	}


        makeGray(srcView);
      //  generateListOfCodebookVectors();
       // process();
	}

	private File openFile() {
        JFileChooser chooser = new JFileChooser("data");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (*.jpg, *.png, *.gif)", "jpg", "png", "gif", "codebook");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(this);
        if(ret == JFileChooser.APPROVE_OPTION) {
            imgName = chooser.getSelectedFile().getName();
            return chooser.getSelectedFile();
        } else {
            System.out.println("No File opened.");
            return null;
        }

	}

	private void loadFile(File file) {
		if(file != null) {
            srcView.loadImage(file);
    		makeGray(srcView);
    		srcView.setMaxSize(new Dimension(maxWidth, maxHeight));
    		recView.resetToSize(srcView.getImgWidth(), srcView.getImgHeight());
    		frame.pack();
		}
	}
    
	private static void createAndShowGUI() {
		// create and setup the window
		frame = new JFrame("Vektorquantisierer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JComponent newContentPane = new Vectorquantization();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        // display the window.
        frame.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);
	}

	public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
	}

	public void makeGray(ImageView imgView) {
		int pixels[] = imgView.getPixels();
		for(int i = 0; i < pixels.length; i++) {
			int gray = ((pixels[i] & 0xff) + ((pixels[i] & 0xff00) >> 8) + ((pixels[i] & 0xff0000) >> 16)) / 3;
			pixels[i] = 0xff000000 | (gray << 16) | (gray << 8) | gray;
		}
		imgView.applyChanges();
	}
	
	private double calcMSE(ImageView view1, ImageView view2) {

		int[] pix1 = view1.getPixels();
		int[] pix2 = view2.getPixels();
		int mse = 0;

		for (int i = 0; i < pix1.length; i++) {
			int p1 = (pix1[i] & 0xff0000) >> 16;
			int p2 = (pix2[i] & 0xff0000) >> 16;

			int error = p1 - p2;
			mse += error * error;
		}
		mse /= pix1.length;
		return mse;
	}

    private double calcMSE(ImageView view1, int[] pix2) {

        int[] pix1 = view1.getPixels();
        int mse = 0;

        for (int i = 0; i < pix1.length; i++) {
            int p1 = (pix1[i] & 0xff0000) >> 16;
            int p2 = (pix2[i] & 0xff0000) >> 16;

            int error = p1 - p2;
            mse += error * error;
        }
        mse /= pix1.length;
        return mse;
    }

    private double calcPSNR(ImageView view1, int[] pix2) {
        double mse = this.calcMSE(view1, pix2);
        double psnr = 10.0d * Math.log10(65025.0d / mse);
        return psnr;
    }
/*
	private void safeCodebook() {
		try {
				FileOutputStream fout = new FileOutputStream("data/"+ imgName + "-numVec" + vectorSide + ".codebook");
				ObjectOutputStream oos = new ObjectOutputStream(fout);   
				oos.writeObject(generateListOfCodebookVectors());
				oos.close();
		   } catch (Exception ex) {
			   ex.printStackTrace();
		   }
	}
	
	private ArrayList<int[][]> loadCodebook(File codebookList) {
		try {
			   FileInputStream fin = new FileInputStream(codebookList);
			   ObjectInputStream ois = new ObjectInputStream(fin);
               listOfCodeBookVectors = (ArrayList<int[][]>) ois.readObject();
			   ois.close();
			   return listOfCodeBookVectors;
		   } catch(Exception ex) {
			   return null;
		   } 
	}
*/
    private void process2() {
        int[][] codedImage = book.codeImage(srcView, codebookVectors);
        int[] decodedImage = book.decodeImage(srcView.getImgWidth(), srcView.getImgHeight(), codedImage, codebookVectors);

        recView.setPixels(decodedImage);
        recView.applyChanges();

        double psnr = calcPSNR(srcView, recView.getPixels());
        double bpp = (Math.log(codebookSize) / Math.log(2)) / vectorSize;
        System.out.println("PSNR: " + psnr);
        System.out.println("BPP: " + bpp);
    }

    private void trainFromSrc() {
        ImgAsVectors loadVec = new ImgAsVectors(srcView, vectorSide);
        double[][] inputVec = loadVec.getVectors2d();
        book.update(vectorSide, codebookSize);
        codebookVectors = book.generateCodebookFromImage2dVector(inputVec);
        process2();
    }

    private void openFolder() {

        JFileChooser chooser = new JFileChooser("/Users/Bernhard/Dropbox/HTW Berlin/Bachelor/Bachelorarbeit/Bachelorarbeit RBM/data");

        // Nur komplette Ordner koennen ausgewaehlt werden
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        String fileNames = "";

        int ret = chooser.showOpenDialog(this);
        if(ret == JFileChooser.APPROVE_OPTION) {
            File[] folders = chooser.getSelectedFiles();

            for (File folder : folders) {
                File[] files = folder.listFiles();
                for (File file : files) {
                    fileNames += file.getName();
                }
            }

            double[][] inputVec = loadFolder(folders);
            book.update(vectorSide, codebookSize);
            codebookVectors = book.generateCodebookFromImage2dVector(inputVec);
            process2();
            /*
            reducedVectors = new Matrix(inputVec.length, inputVec[0].length);
            reducedVectors.setData(inputVec);
            reducedVectors = reducedVectors.divide(255.0d);
            rbm.train(reducedVectors);
            */
        } else {
            System.out.println("No File opened.");
        }
/*
        try {
            File file = new File("/", " " + fileNames + " v:" + rbm.getNumVisible() + "-h:" + rbm.getNumHidden() + "-LR:" + rbm.getLearningRate() + "-E:" + rbm.getMaxEpochs() + ".rbm");
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(rbm);
            oos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        */
    }

    private double[][] loadFolder(File[] folders) {

        ImageView loadView;
        ImgAsVectors loadVec;
        double[][] vectors = new double[0][vectorSize];

        for (File folder : folders) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (!file.getName().equals(".DS_Store")) {
                    loadView = new ImageView(file);
                    loadVec = new ImgAsVectors(loadView, vectorSide);
                    double[][] currentVec = loadVec.getVectors2d(); // hier vectors für vectorquant bekommen
                    int vectorsLength = vectors.length;
                    vectors = Arrays.copyOf(vectors, vectorsLength + currentVec.length);
                   System.arraycopy(currentVec, 0, vectors, vectorsLength, currentVec.length);
                }
            }
        }
        return vectors;
    }
/*
    private ArrayList<int[][]> generateListOfCodebookVectors() {
        listOfCodeBookVectors.clear();
        for (int i = 0; i < blockSizes.length; i++) {
            int blockSizeLocal = blockSizes[i];
            ImgAsVectors srcVectors;
            srcVectors = new ImgAsVectors(srcView, blockSizeLocal);
            book.update(blockSizeLocal, this.vectorSide);
            int[][] codeBookVectors = book.generateCodebookFromImage3dVector(srcVectors.getVectors());
            listOfCodeBookVectors.add(codeBookVectors);
        }
        return listOfCodeBookVectors;
    }
*/
    private void changeView() {
        // 2. Parameter: die Breite eine Blocks des jeweiligen Codebuches
        drawSamples(listOfCodeBookVectors.get(selectedBlockIndex), (int)Math.sqrt(listOfCodeBookVectors.get(selectedBlockIndex)[0].length));

        recView.setPixels(listOfDecodedImages.get(selectedBlockIndex));
        recView.applyChanges();

        negView.setPixels(generateNegativeImage(srcView.getPixels(), recView.getPixels()));
        negView.applyChanges();
    }
	
	private void drawSamples(int[][] codeBookVectors, int blockSide) {
        book.update(blockSide, this.vectorSide);
        ImageView sampleView;
        samplesPanel.removeAll();

        for (int k = 0; k < codeBookVectors.length; k++) {
            sampleView = new ImageView(blockSide, blockSide);

            int[] samplePixels = sampleView.getPixels();
            for (int i = 0; i < samplePixels.length; i++) {
                samplePixels[i] = 0xFF000000 + ((codeBookVectors[k][i] & 0xff) << 16) + ((codeBookVectors[k][i] & 0xff) << 8)
                        + (codeBookVectors[k][i] & 0xff);
            }
            sampleView.setPixels(samplePixels);
            sampleView.applyChanges();
            samplesPanel.add(sampleView);
        }
        samplesPanel.revalidate();
        samplesPanel.repaint();
	}

    private int[] generateNegativeImage(int[] pixels1, int[] pixels2) {
        int[] negPixels = new int[pixels1.length];
        for (int i = 0; i < pixels1.length; i++) {
            int negVal = (pixels1[i] & 0xff) - (pixels2[i] & 0xff) + 128;
            negPixels[i] = 0xFF000000 + ((negVal & 0xff) << 16) + ((negVal & 0xff) << 8)
                    + (negVal & 0xff);
        }
        return negPixels;
    }
}
