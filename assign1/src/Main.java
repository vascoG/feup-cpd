import java.util.Scanner;

public class Main {

    private static void OnMult(int m_ar, int m_br){

        String st;
        double temp;
        int i,j,k;

        double [] pha = new double[m_ar*m_ar];
        double [] phb = new double[m_ar*m_ar];
        double [] phc = new double[m_ar*m_ar];

        for(i=0;i<m_ar;i++)
        {
            for(j=0;j<m_ar;j++)
            {
                pha[i*m_ar+j]=(double) 1.0;
            }
        }
        for(i=0;i<m_br;i++)
        {
            for(j=0;j<m_br;j++)
            {
                phb[i*m_br+j]=(double)(i+1);
            }
        }
        long startTime = System.nanoTime();

        for (i=0;i<m_ar;i++)
        {
            for(j=0;j<m_br;j++)
            {
                temp = 0;
                for (k=0;k<m_ar;k++)
                {
                    temp+=pha[i*m_ar+k]*phb[k*m_br+j];
                }
                phc[i*m_ar+j]=temp;
            }
        }

        long estimatedTime = (System.nanoTime() - startTime);
        double totalTime = estimatedTime/(double)1000000000;
        System.out.println(totalTime);


    }
    private static void OnLineMult(int m_ar, int m_br){

        String st;
        double temp;
        int i,j,k;

        double [] pha = new double[m_ar*m_ar];
        double [] phb = new double[m_ar*m_ar];
        double [] phc = new double[m_ar*m_ar];

        for(i=0;i<m_ar;i++)
        {
            for(j=0;j<m_ar;j++)
            {
                pha[i*m_ar+j]=(double) 1.0;
            }
        }
        for(i=0;i<m_br;i++)
        {
            for(j=0;j<m_br;j++)
            {
                phb[i*m_br+j]=(double)(i+1);
            }
        }
        for(i=0;i<m_br;i++)
        {
            for(j=0;j<m_br;j++)
            {
                phc[i*m_br+j]=(double)0.0;
            }
        }
        long startTime = System.nanoTime();

        for (i=0;i<m_ar;i++)
        {
            for (k=0;k<m_ar;k++)
            {
                for(j=0;j<m_br;j++)
                {
                    phc[i*m_ar+j]+=pha[i*m_ar+k]*phb[k*m_br+j];
                }
                
            }
        }

        long estimatedTime = (System.nanoTime() - startTime);
        double totalTime = estimatedTime/(double)1000000000;
        System.out.println(totalTime);
    }

    public static void main(String[] args) {
	// write your code here
        String st;
        double temp;
        int i,j,k;

        double [][] pha;
        double [][] phb;
        double [][] phc;

        int op=1;
        int lin,col;
        Scanner scan = new Scanner(System.in);
        do{
            System.out.println("\n1.Multiplication");
            System.out.println("\n2.Line Multiplication");
            System.out.println("\n0.Exit");
            System.out.println("\nSelection?: ");
            op = scan.nextInt();
            if(op==0)
                break;
            System.out.println("\nDimensions: lins=cols ? ");
            lin=scan.nextInt();
            col = lin;
            switch (op){
                case 1: OnMult(lin,col);
                break;
                case 2: OnLineMult(lin, col);
                break;
            }
        }while(op!=0);

    }




}
