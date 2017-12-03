/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vision.artificial;

import com.vision.artificial.model.Conexion;
import com.vision.artificial.model.Ejecucion;
import com.vision.artificial.model.EntityModel;
import com.vision.artificial.model.Limon;
import java.io.File;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;
import static org.opencv.core.Core.BORDER_DEFAULT;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * FXML Controller class
 *
 * @author EduardoDev
 */
public class FXMLFullExecutionController{
    
    @FXML
    private Button btnCargarImagenes;
    
    private Scene scene;
    private Stage stage;
    private final FileChooser fileChooser;
    //private File file;
    private final String strRutaResources;

    
    public FXMLFullExecutionController(){
        
        this.fileChooser= new FileChooser();
        
        this.strRutaResources="src/resources/img/base/";
    }
    Mat mImagenReal;  
    Mat mImagen; 
    @FXML
    protected void bulkImages() throws MalformedURLException, SQLException, ClassNotFoundException {
              
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date startDate = new Date();
        
        List<File> list =fileChooser.showOpenMultipleDialog(stage);
        System.out.println("Start : "+dateFormat.format(startDate));
        if (list != null) {

            Ejecucion ejecucion= new Ejecucion();

            EntityModel.guardarEjecucion(Conexion.obtener(), ejecucion);
            
            //<editor-fold defaultstate="collapsed" desc="delete previous file">
            File base= new File(this.strRutaResources);
            for(File file: base.listFiles())
                if (!file.isDirectory())
                    file.delete();
            //</editor-fold>
            Limon limon = null;
            //<editor-fold defaultstate="collapsed" desc="save new files">
            startDate = new Date();
                System.out.println("Init procces : "+dateFormat.format(startDate));
            for (File file : list) {
                
                //System.out.println(file.getName());
                mImagenReal = Imgcodecs.imread(file.getAbsolutePath());
                Imgcodecs.imwrite(strRutaResources+file.getName(),mImagenReal);
                mImagen= new Mat();
                System.out.println(strRutaResources+ file.getName());
                limon= new Limon();
                //obtener id de ejecucion
                limon.setEjecucion(ejecucion.getId());
                limon.setIdentificador(Integer.parseInt(file.getName().split(".jpg")[0]));
                
                mImagen = mImagenReal;//Imgcodecs.imread(strRutaResources+ file.getName());
                ProcesarImagen(/*mImagenAnalisis,*/limon/*,j*/);
                EntityModel.guardarLimon(Conexion.obtener(), limon);
            }
            //</editor-fold>
            
            Conexion.cerrar();
       
        }
        startDate = new Date();
        System.out.println("Finish : "+dateFormat.format(startDate));
    }
    
    public static final double MIN_AREA = 100.00;
    
    public static final double MAX_TOL = 200.00;
    
    Mat mImagenBinarizada;
    Mat mImagenRealRedimensionada;
    Mat mImagenSuavizada ;
    Size tamaño;
    Mat mImagenHSV;
    List<Mat> lstCanalesHSV;
    Size size;
    Mat mCircles;
    Mat mHierarchy;
    List<MatOfPoint> circles;
    private void ProcesarImagen(/*Mat mImagen,*/ Limon limon/*, int side*/){
        mImagenRealRedimensionada= new Mat();
        mImagenSuavizada = new Mat();
        mImagenHSV = new Mat();
        
        tamaño= new Size(3,3);
        
        // <editor-fold defaultstate="collapsed" desc=" Pre Procesar ">
        //1. REDIMENSION
        double ratio=mImagen.size().width/mImagen.size().height;
        int area=500000;
        double altura=Math.sqrt(area/(ratio));
        double anchura=altura*ratio;
        size= new Size(anchura,altura);
        Imgproc.resize(mImagen, mImagenRealRedimensionada, size);
        //Imgcodecs.imwrite(strRutaResources+"img/redimension.jpg",mImagenRealRedimensionada); 
        //2. SUAVIZAR
        Imgproc.GaussianBlur(mImagenRealRedimensionada, mImagenSuavizada, tamaño ,0,0, BORDER_DEFAULT );
        //Imgcodecs.imwrite(strRutaResources+"img/suavizado.jpg",mImagenSuavizada); 
        //3. HSV
        Imgproc.cvtColor(mImagenSuavizada, mImagenHSV, Imgproc.COLOR_BGR2HSV);
        //Imgcodecs.imwrite(strRutaResources+"img/hsv.jpg",mImagenHSV);  
        lstCanalesHSV=new ArrayList<>();
        Core.split(mImagenHSV,lstCanalesHSV);
        //Imgcodecs.imwrite(strRutaResources+"img/H.jpg",lstCanalesHSV.get(0));        
        //Imgcodecs.imwrite(strRutaResources+"img/S.jpg",lstCanalesHSV.get(1));
        //Imgcodecs.imwrite(strRutaResources+"img/V.jpg",lstCanalesHSV.get(2));
        // </editor-fold>
        
        this.mImagenBinarizada=new Mat();
        Mat mImgDilatar = new Mat();
        Mat mImgErode = new Mat();
        Mat mElementoErosion ;
        Mat mElementoDilatacion;
        int tamañoErosion = 1;
        int tamañoDilatacion = 1;
        Scalar minVerde = new Scalar(29, 86, 6);        
        Scalar maxVerde = new Scalar(64/*40*/, 255, 255);
        
        // <editor-fold defaultstate="collapsed" desc=" Segmentar ">
        //1. Binarizar / umbralizar
        Core.inRange(mImagenHSV, minVerde, maxVerde, this.mImagenBinarizada);
        //Imgcodecs.imwrite(strRutaResources+"img/binarizada.jpg",this.mImagenBinarizada);
        //2. Erosiona Binaria
        mElementoErosion = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size( 2*tamañoErosion + 1, 2*tamañoErosion+1 ));
        Imgproc.erode(this.mImagenBinarizada, mImgErode,mElementoErosion);
        //3. Dilatar Binaria
        mElementoDilatacion = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,new Size( 2*tamañoDilatacion + 1, 2*tamañoDilatacion+1 ));
        Imgproc.dilate(mImgErode, mImgDilatar,mElementoDilatacion);
        // </editor-fold>
        
        mHierarchy= new Mat();
        // <editor-fold defaultstate="collapsed" desc=" Descripcion ">
        Mat mClonImagenReal = mImagenRealRedimensionada;
        //List<MatOfPoint> circles;
        circles = new ArrayList<MatOfPoint>();
       
        Imgproc.findContours(/*this.mImagenBinarizada*/ mImgDilatar, circles,mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for( int i = 0; i< circles.size(); i++ )
        {
            double actual_area = Math.abs(Imgproc.contourArea(circles.get(i)));
            if (actual_area < MIN_AREA) continue;
            
            Rect rect = Imgproc.boundingRect(circles.get(i));
            int A = rect.width  / 2;
            int B = rect.height / 2;
            double estimated_area = Math.PI * A * B;
            double error = Math.abs(actual_area - estimated_area);
            
            if (error > MAX_TOL) continue;
           
            System.out.printf("center x: %d y: %d A: %d B: %d\n", rect.x + A, rect.y + B, A, B);
            Scalar color = new Scalar( 0,255,255);
            Imgproc.drawContours( mClonImagenReal, circles, i, color, 2, 8, mHierarchy, 0, new Point() );
        }   
        //Imgcodecs.imwrite(strRutaResources+"img/deteccionCirculo.jpg",mClonImagenReal);
        // </editor-fold>
        
        
        double diametro=0;
        // <editor-fold defaultstate="collapsed" desc=" reconocimiento ">
        
        Mat threshold_output= new Mat();
        List<MatOfPoint> lstContours = new ArrayList<MatOfPoint>();
        Mat hierarchy= new Mat();
     
        Imgproc.blur(lstCanalesHSV.get(2), lstCanalesHSV.get(2), new Size(3,3));
        Imgproc.adaptiveThreshold(lstCanalesHSV.get(2), threshold_output,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY, 11, 2);
        
        //Imgcodecs.imwrite(strRutaResources+"img/_threshold.jpg",threshold_output);
        
        Imgproc.findContours(mImagenBinarizada, lstContours, hierarchy, Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE,new Point(0,0));
        
        List<MatOfPoint> contours_poly = new ArrayList<MatOfPoint>();
        List<Point> center= new ArrayList<Point>();
        for (int i = 0; i < lstContours.size(); i++) {
            contours_poly.add(new MatOfPoint());
            center.add(new Point());
          }
        
        
        Rect[] boundRect = new Rect[lstContours.size()];
        
        float[] radius = new float[lstContours.size()];
        MatOfPoint2f mop2 = null;         
        MatOfPoint2f mop2_1 = null;           
        MatOfPoint mop_1 = null;       
    
      
        for( int i = 0; i < lstContours.size(); i++ )
        { 
            mop2 = new MatOfPoint2f();
            lstContours.get(i).convertTo(mop2, CvType.CV_32FC2);
            
            mop2_1 = new MatOfPoint2f();
            contours_poly.get(i).convertTo(mop2_1, CvType.CV_32FC2);
            
            mop_1= new MatOfPoint();
            contours_poly.get(i).convertTo(mop_1, CvType.CV_32S);
            
            Imgproc.approxPolyDP(mop2,mop2_1, 3, true);
            //approxPolyDP( Mat(lstContours[i]), contours_poly[i], 3, true );
            boundRect[i] = Imgproc.boundingRect(mop_1);
            
            Imgproc.minEnclosingCircle(mop2_1, center.get(i), radius); 
            //minEnclosingCircle( (Mat)contours_poly[i], center[i], radius[i] );
        }
        
        Mat drawing = new Mat( threshold_output.size(),CvType.CV_8UC3 );
        for( int i = 0; i< lstContours.size(); i++ )
        {
          Scalar color = new Scalar(255,52,64);
          Imgproc.drawContours(drawing, lstContours, i, color,1,8, new Mat(),0, new Point()); 
          //drawContours( drawing, contours_poly, i, color, 1, 8, vector<Vec4i>(), 0, Point() );
          Imgproc.rectangle( drawing, boundRect[i].tl(), boundRect[i].br(), color, 2, 8, 0 );
          Imgproc.circle(drawing, center.get(i), (int)radius[i], color, 2, 8, 0 );
          
            if (radius[i]>0){
            //System.out.println("Radio :"+radius[i]*2); //0.26454            
            //System.out.println("Diametro :"+radius[i]*2*0.29853); //0.26454
            diametro=radius[i]*2*0.29853;
            }

        }
        //Imgcodecs.imwrite(strRutaResources+"img/deteccionEllipseCuadro.jpg",drawing); 
        limon.setDiametroA(diametro);
        
        
        /*mCircles= new Mat();
        int minRadius = 30;//10
	int maxRadius = 250;//18
        Imgproc.HoughCircles(lstCanalesHSV.get(1), mCircles,Imgproc.CV_HOUGH_GRADIENT,1, 
                            200, 150, 30, minRadius, maxRadius);//this.mImagenBinarizada
        
        for( int i = 0; i < mCircles.cols(); i++ )
	{
            double vCircle[]=mCircles.get(0,i);
            
            Point center=new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
            int radius = (int)Math.round(vCircle[2]);
            Imgproc.circle(mClonImagenReal, center, 3,new Scalar(0,255,0), -1, 8, 0 );
            //(mImagenSuavizada, center, 3,sc1new Scalar(0,255,0), -1, 8, 0 )
            // draw the circle outline
            Imgproc.circle(mClonImagenReal, center, radius, new Scalar(0,0,255),3, 8, 0 );
            
            //System.out.println(vCircle[0]+" : "+vCircle[1]+" : "+vCircle[2]);//el tercero es el radio en px
            diametro=vCircle[2]*2*0.26454;
	}
        Imgcodecs.imwrite(strRutaResources+"img/deteccionDiametro.jpg",mClonImagenReal);
        limon.setDiametroA(diametro);*/

    }

    
}
