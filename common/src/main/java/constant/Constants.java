package constant;

import java.time.format.DateTimeFormatter;

public final class Constants {

  public static final String PROFILE_PROD = "prod";
  public static final String DEFAULT = "default";

  public static final String EXTRA_REQUEST_ID = "extra_request_id";
  public static final String HEADER_EXTRA_REQUEST_ID = "X_Extra_Request_Id";

  public static final String REQUEST_ID = "request_id";
  public static final String HEADER_REQUEST_ID = "X_Request_Id";


  public static final String NOT_APPLICABLE = "";
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


  public static final int CHUNK_SIZE = 100;
  public static final int SKIP_LIMIT = 10;

  private Constants() {
  }
}
