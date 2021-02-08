import com.alibaba.fastjson.JSON;
import com.swust.common.entity.FileInfo;
import com.swust.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * date   2021/2/8 13:53
 */
@Slf4j
public class FileBlockTest {

    @Test
    public void testBlock() {
        File file = new File("E:/test1.txt");
        log.info("原始文件大小 : {} B", file.length());
        //512kb
        FileInfo fileInfo = FileUtil.blockFile(file, 1024 * 512);
        log.info(JSON.toJSONString(fileInfo));
        log.info("文件信息大小 : {} B", JSON.toJSONString(fileInfo).length());

    }
}