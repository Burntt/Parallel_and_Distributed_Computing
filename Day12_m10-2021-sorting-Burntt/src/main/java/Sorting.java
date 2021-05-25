import java.util.Arrays;

public class Sorting {
    public void yourSuperSortAlgorithm(double[] values) throws InterruptedException {

        int numberOfThreads = 2;
        int partLength = values.length / numberOfThreads;

        double [] arrayValuesPart1 = new double[partLength];
        double [] arrayValuesPart2 = new double[partLength];

        System.arraycopy(values, 0, arrayValuesPart1, 0, partLength);
        System.arraycopy(values, partLength, arrayValuesPart2, 0, partLength);

        Runnable executableValues1 = new Runnable() {
            @Override
            public void run() {
                Arrays.sort(arrayValuesPart1);
            }
        };

        Runnable executableValues2 = new Runnable() {
            @Override
            public void run() {
                Arrays.sort(arrayValuesPart2);
            }
        };

        Thread threadValues1 = new Thread(executableValues1);
        Thread threadValues2 = new Thread(executableValues2);

        threadValues1.start();
        threadValues2.start();

        threadValues1.join();
        threadValues2.join();
        mergeSortedArrays(values, arrayValuesPart1, arrayValuesPart2);

    }

    public static void mergeSortedArrays(double[] answer, double[] sortedArray1, double[] sortedArray2) {

        int i = 0;
        int j = 0;
        int k = 0;

        while (!(i >= sortedArray1.length) && !(j >= sortedArray2.length)) {
            if (sortedArray2[j] > sortedArray1[i]) {
                answer[k++] = sortedArray1[i++];
            } else {
                answer[k++] = sortedArray2[j++];
            }
        }

        while (i < sortedArray1.length){
            answer[k++] = sortedArray1[i++];
        }

        while (j < sortedArray2.length){
            answer[k++] = sortedArray2[j++];
        }
    }
}


