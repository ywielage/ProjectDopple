package com.inf2c.doppleapp;

import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.conversion.DoppleRawDataObject;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void decode() {
        DoppleConversion converter = new DoppleConversion();
        String DataString = "A30B1300000000008814C1112AFEE814A910DFFEFC14AA0FE1FEA815A20FE2FE90172E1043009C17B710D9FF4C16C3135AFD64159B15EBFB7C13ED14ADFBC813AD160EFB1013D51A2FF9E011961B71F814124A1C06F814157A1BCFF8E8142B1B89F8DC14AF1AE6F81416CF1ABFF92815951845FA14168115D2FB60192D12C3FE";
        DoppleRawDataObject object = converter.convert(DataString);

        double averageX = converter.getAverageXValue(object);
        double averageY = converter.getAverageYValue(object);
        double averageZ = converter.getAverageZValue(object);

        assertEquals("Timestamp was not correct.", 1248163d, object.Timestamp, 0.01d);
        assertEquals("X0 was not correct.", 1314, (int)object.doppleData.get("X0"));
        assertEquals("Y0 was not correct.", 1136, (int)object.doppleData.get("Y0"));
        assertEquals("Z0 was not correct.", -118, (int)object.doppleData.get("Z0"));
        assertEquals("X1 was not correct.", 1338, (int)object.doppleData.get("X1"));
        assertEquals("Y1 was not correct.", 1066, (int)object.doppleData.get("Y1"));

    }

    @Test
    public void TestAcceleration(){
        int x = 1528;
        int y = 1240;
        int z = -380;
        DoppleConversion converter = new DoppleConversion();
        double acc = converter.acceleration(x, y, z);
        assertEquals(0.018744d, acc, 0.00001d);
    }

    @Test
    public void outputThis(){

        StringBuilder builder = new StringBuilder();
        //get xyz * 20
        for(int i = 0; i < 20; i++){
            //get x
            builder.append("X" + i).append(",");
            builder.append("X_CNT" + i).append(",");
            //get y
            builder.append("Y" + i).append(",");
            builder.append("Y_CNT" + i).append(",");
            //get z
            builder.append("Z" + i).append(",");
            builder.append("Z_CNT" + i).append(",");
        }
        System.out.println(builder.toString());
    }

    @Test
    public void getDistance() {
        DoppleConversion conversion = new DoppleConversion();
        DoppleDataObject wayp1 = new DoppleDataObject(100, 100, 1, 1, 1);
        DoppleDataObject wayp2 = new DoppleDataObject(100, 100, 1, 1, 1);

        wayp1.Lat = 51.5;
        wayp1.Long = 0;

        wayp2.Lat = 38.8;
        wayp2.Long = -77.1;

        assertEquals(5918.185064088764, conversion.calculateDistance(wayp1, wayp2), 0.00000001);
    }
}